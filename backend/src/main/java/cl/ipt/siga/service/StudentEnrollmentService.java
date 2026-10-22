package cl.ipt.siga.service;

import cl.ipt.siga.dto.CourseEnrollmentResponse;
import cl.ipt.siga.dto.EnrollmentSelectionRequest;
import cl.ipt.siga.dto.SectionResponse;
import cl.ipt.siga.entity.AcademicPeriod;
import cl.ipt.siga.entity.Course;
import cl.ipt.siga.entity.CourseEnrollment;
import cl.ipt.siga.entity.Section;
import cl.ipt.siga.entity.Student;
import cl.ipt.siga.entity.StudentMatriculation;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import cl.ipt.siga.entity.enums.CourseEnrollmentStatus;
import cl.ipt.siga.entity.enums.StudentStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.AcademicPeriodRepository;
import cl.ipt.siga.repository.CourseEnrollmentRepository;
import cl.ipt.siga.repository.CourseRepository;
import cl.ipt.siga.repository.SectionRepository;
import cl.ipt.siga.repository.StudentMatriculationRepository;
import cl.ipt.siga.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class StudentEnrollmentService {

    private static final BigDecimal PASSING_GRADE = BigDecimal.valueOf(4);

    private final StudentRepository studentRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final StudentMatriculationRepository matriculationRepository;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    public StudentEnrollmentService(
            StudentRepository studentRepository,
            AcademicPeriodRepository academicPeriodRepository,
            StudentMatriculationRepository matriculationRepository,
            CourseRepository courseRepository,
            SectionRepository sectionRepository,
            CourseEnrollmentRepository enrollmentRepository
    ) {
        this.studentRepository = studentRepository;
        this.academicPeriodRepository = academicPeriodRepository;
        this.matriculationRepository = matriculationRepository;
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> findOfferings(String keycloakSubject, Long periodId) {
        Student student = getStudentBySubject(keycloakSubject);
        getPeriod(periodId);
        Set<Long> passedCourses = enrollmentRepository.findPassedCourseIdsByStudentId(student.getId());

        return sectionRepository.findByAcademicPeriodIdOrderByCourseCodeAsc(periodId).stream()
                .filter(section -> belongsToStudentPlan(student, section.getCourse()))
                .filter(section -> !passedCourses.contains(section.getCourse().getId()))
                .filter(section -> prerequisitesPassed(student.getId(), section.getCourse()))
                .filter(section -> hasSeatForStudent(section, student.getId()))
                .map(SectionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseEnrollmentResponse> findEnrollments(String keycloakSubject, Long periodId) {
        Student student = getStudentBySubject(keycloakSubject);
        getPeriod(periodId);
        return enrollmentRepository.findByStudentIdAndAcademicPeriodIdOrderByRegisteredAtAsc(
                        student.getId(),
                        periodId
                ).stream()
                .map(CourseEnrollmentResponse::from)
                .toList();
    }

    public List<CourseEnrollmentResponse> replaceEnrollments(
            String keycloakSubject,
            Long periodId,
            EnrollmentSelectionRequest request
    ) {
        Student student = getStudentBySubjectForUpdate(keycloakSubject);
        AcademicPeriod period = getOpenPeriod(periodId);
        ensureStudentMayEnroll(student, periodId);

        List<Long> requestedIds = request.sectionIds();
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new BusinessConflictException("A section can only be selected once.");
        }
        List<Section> sections = lockSections(requestedIds);
        Map<Long, CourseEnrollment> existingByCourse = new LinkedHashMap<>();
        enrollmentRepository.findByStudentIdAndAcademicPeriodIdOrderByRegisteredAtAsc(student.getId(), periodId)
                .forEach(enrollment -> existingByCourse.put(enrollment.getCourse().getId(), enrollment));

        Set<Long> passedCourses = enrollmentRepository.findPassedCourseIdsByStudentId(student.getId());
        List<Course> planCourses = courseRepository.findByStudyPlanIdOrderBySemesterAscCodeAsc(
                student.getStudyPlan().getId()
        );
        validateSelection(student, period, sections, requestedIds, existingByCourse, passedCourses, planCourses);

        Set<Long> selectedCourseIds = sections.stream()
                .map(section -> section.getCourse().getId())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (CourseEnrollment previous : existingByCourse.values()) {
            if (previous.getStatus() == CourseEnrollmentStatus.ENROLLED
                    && !selectedCourseIds.contains(previous.getCourse().getId())) {
                if (previous.getFinalGrade() != null) {
                    throw new BusinessConflictException("An enrollment with a final grade cannot be cancelled.");
                }
                previous.setStatus(CourseEnrollmentStatus.CANCELLED);
            }
        }

        List<CourseEnrollment> updated = new ArrayList<>();
        for (Section section : sections) {
            CourseEnrollment enrollment = existingByCourse.get(section.getCourse().getId());
            if (enrollment == null) {
                enrollment = new CourseEnrollment(student, section.getCourse(), section, period);
            } else if (enrollment.getStatus() == CourseEnrollmentStatus.CANCELLED) {
                enrollment.setStatus(CourseEnrollmentStatus.ENROLLED);
                enrollment.setRegisteredAt(Instant.now());
                enrollment.setSection(section);
            }
            updated.add(enrollment);
        }

        enrollmentRepository.saveAll(existingByCourse.values());
        enrollmentRepository.saveAll(updated);
        return enrollmentRepository.findByStudentIdAndAcademicPeriodIdOrderByRegisteredAtAsc(student.getId(), periodId)
                .stream()
                .map(CourseEnrollmentResponse::from)
                .toList();
    }

    public CourseEnrollmentResponse cancelEnrollment(String keycloakSubject, Long periodId, Long enrollmentId) {
        Student student = getStudentBySubjectForUpdate(keycloakSubject);
        getOpenPeriod(periodId);
        CourseEnrollment enrollment = enrollmentRepository.findByIdForUpdate(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));
        if (!enrollment.getStudent().getId().equals(student.getId())
                || !enrollment.getAcademicPeriod().getId().equals(periodId)) {
            throw new ResourceNotFoundException("Enrollment not found: " + enrollmentId);
        }
        if (enrollment.getFinalGrade() != null) {
            throw new BusinessConflictException("An enrollment with a final grade cannot be cancelled.");
        }
        enrollment.setStatus(CourseEnrollmentStatus.CANCELLED);
        return CourseEnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    private void validateSelection(
            Student student,
            AcademicPeriod period,
            List<Section> sections,
            List<Long> requestedIds,
            Map<Long, CourseEnrollment> existingByCourse,
            Set<Long> passedCourses,
            List<Course> planCourses
    ) {
        if (sections.size() < requestedIds.size()) {
            throw new ResourceNotFoundException("One or more sections were not found.");
        }

        Set<Long> selectedCourseIds = new HashSet<>();
        Set<String> selectedScheduleBlocks = new HashSet<>();
        for (Section section : sections) {
            Course course = section.getCourse();
            if (!section.getAcademicPeriod().getId().equals(period.getId())) {
                throw new BusinessConflictException("Every selected section must belong to the requested period.");
            }
            if (!belongsToStudentPlan(student, course)) {
                throw new BusinessConflictException("Students can only enroll in courses from their study plan.");
            }
            if (!selectedCourseIds.add(course.getId())) {
                throw new BusinessConflictException("A course can only be selected once in an academic period.");
            }
            if (passedCourses.contains(course.getId())) {
                throw new BusinessConflictException("A course that has already been passed cannot be enrolled again.");
            }
            if (!prerequisitesPassed(student.getId(), course)) {
                throw new BusinessConflictException("All prerequisites must be passed before enrolling in a course.");
            }
            if (section.getScheduleBlocks().stream().anyMatch(selectedScheduleBlocks::contains)) {
                throw new BusinessConflictException("Selected sections cannot have conflicting schedules.");
            }
            selectedScheduleBlocks.addAll(section.getScheduleBlocks());

            CourseEnrollment previous = existingByCourse.get(course.getId());
            if (previous != null && previous.getFinalGrade() != null) {
                throw new BusinessConflictException("A graded enrollment cannot be changed.");
            }
            if (!hasSeatForStudent(section, student.getId())) {
                throw new BusinessConflictException("The selected section has no available seats.");
            }
        }

        validateCourseLoad(sections.size(), planCourses, passedCourses);
        validateSemesterProgress(student, period.getId(), sections, passedCourses);
    }

    private void validateCourseLoad(int selectedCount, List<Course> planCourses, Set<Long> passedCourses) {
        long remainingCourses = planCourses.stream()
                .filter(course -> !passedCourses.contains(course.getId()))
                .count();
        if (remainingCourses == 0) {
            throw new BusinessConflictException("The student has already completed all courses in the study plan.");
        }
        int minimum = remainingCourses < 3 ? 1 : 3;
        int maximum = (int) Math.min(6, remainingCourses);
        if (selectedCount < minimum || selectedCount > maximum) {
            throw new BusinessConflictException(
                    "Students must enroll in " + minimum + " to " + maximum + " courses for this period."
            );
        }
    }

    private void validateSemesterProgress(
            Student student,
            Long periodId,
            List<Section> selectedSections,
            Set<Long> passedCourses
    ) {
        List<Section> offeredSections = sectionRepository.findByAcademicPeriodIdOrderByCourseCodeAsc(periodId).stream()
                .filter(section -> belongsToStudentPlan(student, section.getCourse()))
                .filter(section -> !passedCourses.contains(section.getCourse().getId()))
                .filter(section -> prerequisitesPassed(student.getId(), section.getCourse()))
                .filter(section -> hasSeatForStudent(section, student.getId()))
                .toList();
        Set<Long> selectedCourseIds = selectedSections.stream()
                .map(section -> section.getCourse().getId())
                .collect(java.util.stream.Collectors.toSet());

        for (Section selected : selectedSections) {
            int selectedSemester = selected.getCourse().getSemester();
            boolean lowerCourseAvailableButNotSelected = offeredSections.stream()
                    .map(Section::getCourse)
                    .anyMatch(course -> course.getSemester() < selectedSemester
                            && !selectedCourseIds.contains(course.getId()));
            if (lowerCourseAvailableButNotSelected) {
                throw new BusinessConflictException(
                        "Available courses from lower semesters must be selected before higher-semester courses."
                );
            }
        }
    }

    private List<Section> lockSections(List<Long> requestedIds) {
        Map<Long, Section> sectionsById = new LinkedHashMap<>();
        requestedIds.stream().sorted().forEach(id -> sectionRepository.findByIdForUpdate(id)
                .ifPresent(section -> sectionsById.put(id, section)));
        return requestedIds.stream()
                .map(sectionsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private void ensureStudentMayEnroll(Student student, Long periodId) {
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new BusinessConflictException("Only regular students can enroll in courses.");
        }
        StudentMatriculation matriculation = matriculationRepository
                .findByStudentIdAndAcademicPeriodId(student.getId(), periodId)
                .orElseThrow(() -> new BusinessConflictException(
                        "The student is not matriculated in this academic period."
                ));
        if (!matriculation.isMatriculated()) {
            throw new BusinessConflictException("The student is not matriculated in this academic period.");
        }
    }

    private boolean prerequisitesPassed(Long studentId, Course course) {
        return course.getPrerequisites().stream().allMatch(prerequisite ->
                enrollmentRepository.existsByStudentIdAndCourseIdAndFinalGradeGreaterThanEqual(
                        studentId,
                        prerequisite.getId(),
                        PASSING_GRADE
                )
        );
    }

    private boolean hasSeatForStudent(Section section, Long studentId) {
        int occupiedSeats = enrollmentRepository.countBySectionIdAndStatus(
                section.getId(),
                CourseEnrollmentStatus.ENROLLED
        );
        boolean alreadyOccupiesSeat = enrollmentRepository
                .findByStudentIdAndAcademicPeriodIdAndCourseId(
                        studentId,
                        section.getAcademicPeriod().getId(),
                        section.getCourse().getId()
                )
                .filter(enrollment -> enrollment.getStatus() == CourseEnrollmentStatus.ENROLLED)
                .filter(enrollment -> enrollment.getSection().getId().equals(section.getId()))
                .isPresent();
        return occupiedSeats - (alreadyOccupiesSeat ? 1 : 0) < section.getCapacity();
    }

    private boolean belongsToStudentPlan(Student student, Course course) {
        return course.getStudyPlan().getId().equals(student.getStudyPlan().getId());
    }

    private Student getStudentBySubject(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new BusinessConflictException("The authenticated account has no Keycloak subject.");
        }
        return studentRepository.findByKeycloakSubject(subject)
                .orElseThrow(() -> new BusinessConflictException(
                        "The authenticated Keycloak account is not linked to a student."
                ));
    }

    private Student getStudentBySubjectForUpdate(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new BusinessConflictException("The authenticated account has no Keycloak subject.");
        }
        return studentRepository.findByKeycloakSubjectForUpdate(subject)
                .orElseThrow(() -> new BusinessConflictException(
                        "The authenticated Keycloak account is not linked to a student."
                ));
    }

    private AcademicPeriod getOpenPeriod(Long id) {
        AcademicPeriod period = academicPeriodRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + id));
        if (period.getStatus() != AcademicPeriodStatus.OPEN) {
            throw new BusinessConflictException("Enrollments can only be changed in an open period.");
        }
        return period;
    }

    private AcademicPeriod getPeriod(Long id) {
        return academicPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + id));
    }
}
