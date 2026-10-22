package cl.ipt.siga.service;

import cl.ipt.siga.dto.CourseRequest;
import cl.ipt.siga.dto.CourseResponse;
import cl.ipt.siga.entity.Course;
import cl.ipt.siga.entity.StudyPlan;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CourseRepository;
import cl.ipt.siga.repository.CourseEnrollmentRepository;
import cl.ipt.siga.repository.StudyPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final StudyPlanRepository studyPlanRepository;

    public CourseService(
            CourseRepository courseRepository,
            CourseEnrollmentRepository enrollmentRepository,
            StudyPlanRepository studyPlanRepository
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studyPlanRepository = studyPlanRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findByStudyPlan(Long studyPlanId) {
        getStudyPlan(studyPlanId);
        return courseRepository.findByStudyPlanIdOrderBySemesterAscCodeAsc(studyPlanId).stream()
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(Long id) {
        return CourseResponse.from(getCourse(id));
    }

    public CourseResponse create(Long studyPlanId, CourseRequest request) {
        StudyPlan studyPlan = getStudyPlan(studyPlanId);
        String code = normalizeCode(request.code());
        ensureCodeAvailable(studyPlanId, code, null);
        validateCourseValues(request);
        Set<Course> prerequisites = resolvePrerequisites(studyPlan, request);

        Course course = new Course(
                code,
                request.name().trim(),
                request.semester(),
                request.theoryHours(),
                request.exerciseHours(),
                request.laboratoryHours(),
                request.credits(),
                studyPlan
        );
        course.setPrerequisites(prerequisites);
        return CourseResponse.from(courseRepository.save(course));
    }

    public CourseResponse update(Long id, CourseRequest request) {
        Course course = courseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
        if (enrollmentRepository.existsInAcademicHistory(id)) {
            throw new BusinessConflictException("A course with academic history cannot be modified.");
        }
        String code = normalizeCode(request.code());
        ensureCodeAvailable(course.getStudyPlan().getId(), code, id);
        validateCourseValues(request);
        Set<Course> prerequisites = resolvePrerequisites(course.getStudyPlan(), request, id);
        validateDependentCourses(id, request.semester());

        course.setCode(code);
        course.setName(request.name().trim());
        course.setSemester(request.semester());
        course.setTheoryHours(request.theoryHours());
        course.setExerciseHours(request.exerciseHours());
        course.setLaboratoryHours(request.laboratoryHours());
        course.setCredits(request.credits());
        course.setPrerequisites(prerequisites);
        return CourseResponse.from(courseRepository.save(course));
    }

    private Set<Course> resolvePrerequisites(StudyPlan studyPlan, CourseRequest request) {
        return resolvePrerequisites(studyPlan, request, null);
    }

    private Set<Course> resolvePrerequisites(StudyPlan studyPlan, CourseRequest request, Long courseId) {
        if (request.prerequisiteIds().size() > 3) {
            throw new BusinessConflictException("A course cannot have more than three prerequisites.");
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(request.prerequisiteIds());
        if (uniqueIds.size() != request.prerequisiteIds().size()) {
            throw new BusinessConflictException("Prerequisites cannot contain duplicates.");
        }

        Set<Course> prerequisites = new LinkedHashSet<>();
        for (Long prerequisiteId : uniqueIds) {
            Course prerequisite = getCourse(prerequisiteId);
            if (courseId != null && courseId.equals(prerequisiteId)) {
                throw new BusinessConflictException("A course cannot be its own prerequisite.");
            }
            if (!prerequisite.getStudyPlan().getId().equals(studyPlan.getId())) {
                throw new BusinessConflictException("Prerequisites must belong to the same study plan.");
            }
            if (prerequisite.getSemester() >= request.semester()) {
                throw new BusinessConflictException("Prerequisites must be in an earlier semester.");
            }
            prerequisites.add(prerequisite);
        }
        return prerequisites;
    }

    private void validateCourseValues(CourseRequest request) {
        if (request.semester() < 1 || request.semester() > 4) {
            throw new BusinessConflictException("The semester must be between 1 and 4.");
        }
        if (request.credits() < 1 || request.credits() > 7) {
            throw new BusinessConflictException("SCT credits must be between 1 and 7.");
        }
        if (request.theoryHours() < 0 || request.exerciseHours() < 0 || request.laboratoryHours() < 0) {
            throw new BusinessConflictException("TEL hours cannot be negative.");
        }
        if (request.theoryHours() + request.exerciseHours() + request.laboratoryHours() >= 8) {
            throw new BusinessConflictException("The total weekly TEL hours must be less than 8.");
        }
    }

    private void validateDependentCourses(Long courseId, int newSemester) {
        if (courseId == null) {
            return;
        }
        boolean invalidatesPrerequisite = courseRepository.findByPrerequisitesId(courseId).stream()
                .anyMatch(dependent -> dependent.getSemester() <= newSemester);
        if (invalidatesPrerequisite) {
            throw new BusinessConflictException(
                    "The semester cannot be changed because another course depends on this prerequisite."
            );
        }
    }

    private void ensureCodeAvailable(Long studyPlanId, String code, Long excludedCourseId) {
        courseRepository.findByStudyPlanIdAndCode(studyPlanId, code)
                .filter(existing -> !existing.getId().equals(excludedCourseId))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("A course with this code already exists in the study plan.");
                });
    }

    private StudyPlan getStudyPlan(Long id) {
        return studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found: " + id));
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
