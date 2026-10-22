package cl.ipt.siga.service;

import cl.ipt.siga.dto.SectionRequest;
import cl.ipt.siga.dto.SectionResponse;
import cl.ipt.siga.entity.AcademicPeriod;
import cl.ipt.siga.entity.Course;
import cl.ipt.siga.entity.Section;
import cl.ipt.siga.entity.Teacher;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import cl.ipt.siga.entity.enums.CourseEnrollmentStatus;
import cl.ipt.siga.entity.enums.TeacherStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.AcademicPeriodRepository;
import cl.ipt.siga.repository.CourseEnrollmentRepository;
import cl.ipt.siga.repository.CourseRepository;
import cl.ipt.siga.repository.SectionRepository;
import cl.ipt.siga.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional
public class SectionService {

    private static final Pattern VALID_SCHEDULE_BLOCK = Pattern.compile("^[LMWJV][1-6]$");

    private final SectionRepository sectionRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    public SectionService(
            SectionRepository sectionRepository,
            AcademicPeriodRepository academicPeriodRepository,
            CourseRepository courseRepository,
            TeacherRepository teacherRepository,
            CourseEnrollmentRepository enrollmentRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.academicPeriodRepository = academicPeriodRepository;
        this.courseRepository = courseRepository;
        this.teacherRepository = teacherRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> findByPeriod(Long periodId) {
        getPeriod(periodId);
        return sectionRepository.findByAcademicPeriodIdOrderByCourseCodeAsc(periodId).stream()
                .map(SectionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SectionResponse findById(Long id) {
        return SectionResponse.from(getSection(id));
    }

    public SectionResponse create(Long periodId, SectionRequest request) {
        AcademicPeriod period = getOpenPeriod(periodId);
        validateCapacity(request.capacity());
        Course course = getCourse(request.courseId());
        Teacher teacher = getTeacher(request.teacherId());
        ensureCourseAvailable(periodId, request.courseId(), null);
        Set<String> scheduleBlocks = validateSchedule(course, request.scheduleBlocks());
        ensureNoTeacherScheduleConflict(teacher.getId(), periodId, scheduleBlocks, null);

        Section section = new Section();
        section.setAcademicPeriod(period);
        section.setCourse(course);
        section.setTeacher(teacher);
        section.setCapacity(request.capacity());
        section.setScheduleBlocks(scheduleBlocks);
        return SectionResponse.from(sectionRepository.save(section));
    }

    public SectionResponse update(Long id, SectionRequest request) {
        Section currentSection = getSection(id);
        AcademicPeriod period = getOpenPeriod(currentSection.getAcademicPeriod().getId());
        Section section = sectionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + id));
        validateCapacity(request.capacity());
        Course course = getCourse(request.courseId());
        Teacher teacher = getTeacher(request.teacherId());
        ensureCourseAvailable(period.getId(), request.courseId(), id);
        Set<String> scheduleBlocks = validateSchedule(course, request.scheduleBlocks());
        ensureNoTeacherScheduleConflict(teacher.getId(), period.getId(), scheduleBlocks, id);
        ensureSectionChangesAllowed(section, course, request.capacity(), scheduleBlocks);

        section.setCourse(course);
        section.setTeacher(teacher);
        section.setCapacity(request.capacity());
        section.setScheduleBlocks(scheduleBlocks);
        return SectionResponse.from(sectionRepository.save(section));
    }

    private void ensureSectionChangesAllowed(
            Section section,
            Course requestedCourse,
            int requestedCapacity,
            Set<String> requestedSchedule
    ) {
        int enrolledCount = enrollmentRepository.countBySectionIdAndStatus(
                section.getId(),
                CourseEnrollmentStatus.ENROLLED
        );
        if (requestedCapacity < enrolledCount) {
            throw new BusinessConflictException("Section capacity cannot be lower than its current enrollment.");
        }
        if (enrolledCount > 0 && (!section.getCourse().getId().equals(requestedCourse.getId())
                || !section.getScheduleBlocks().equals(requestedSchedule))) {
            throw new BusinessConflictException(
                    "A section with enrolled students cannot change its course or schedule."
            );
        }
    }

    private void validateCapacity(int capacity) {
        if (capacity < 1 || capacity > 60) {
            throw new BusinessConflictException("Section capacity must be between 1 and 60.");
        }
    }

    private Set<String> validateSchedule(Course course, List<String> requestedBlocks) {
        if (requestedBlocks == null || requestedBlocks.isEmpty()) {
            throw new BusinessConflictException("A section must have at least one schedule block.");
        }
        Set<String> blocks = new LinkedHashSet<>();
        for (String requestedBlock : requestedBlocks) {
            String block = requestedBlock == null ? "" : requestedBlock.trim().toUpperCase(Locale.ROOT);
            if (!VALID_SCHEDULE_BLOCK.matcher(block).matches()) {
                throw new BusinessConflictException(
                        "Schedule blocks must use a valid weekday (L, M, W, J, V) and module (1-6)."
                );
            }
            if (!blocks.add(block)) {
                throw new BusinessConflictException("Schedule blocks cannot contain duplicates.");
            }
        }

        int weeklyTelHours = course.getTheoryHours() + course.getExerciseHours() + course.getLaboratoryHours();
        if (weeklyTelHours <= 0 || weeklyTelHours % 2 != 0 || blocks.size() != weeklyTelHours / 2) {
            throw new BusinessConflictException(
                    "The number of schedule blocks must equal the course's weekly TEL hours divided by 2."
            );
        }
        return blocks;
    }

    private void ensureNoTeacherScheduleConflict(
            Long teacherId,
            Long periodId,
            Set<String> requestedBlocks,
            Long excludedSectionId
    ) {
        boolean hasConflict = sectionRepository.findByTeacherIdAndAcademicPeriodId(teacherId, periodId).stream()
                .filter(section -> !section.getId().equals(excludedSectionId))
                .anyMatch(section -> section.getScheduleBlocks().stream().anyMatch(requestedBlocks::contains));
        if (hasConflict) {
            throw new BusinessConflictException("The teacher already has a section during one of these schedule blocks.");
        }
    }

    private void ensureCourseAvailable(Long periodId, Long courseId, Long excludedSectionId) {
        sectionRepository.findByAcademicPeriodIdAndCourseId(periodId, courseId)
                .filter(existing -> !existing.getId().equals(excludedSectionId))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("A course can have only one section in an academic period.");
                });
    }

    private AcademicPeriod getOpenPeriod(Long id) {
        AcademicPeriod period = academicPeriodRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + id));
        if (period.getStatus() != AcademicPeriodStatus.OPEN) {
            throw new BusinessConflictException("Sections can only be created or updated in an open period.");
        }
        return period;
    }

    private AcademicPeriod getPeriod(Long id) {
        return academicPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + id));
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }

    private Teacher getTeacher(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + id));
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessConflictException("Only active teachers can be assigned to sections.");
        }
        return teacher;
    }

    private Section getSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + id));
    }
}
