package cl.ipt.siga.service;

import cl.ipt.siga.dto.AcademicPeriodRequest;
import cl.ipt.siga.dto.AcademicPeriodResponse;
import cl.ipt.siga.entity.AcademicPeriod;
import cl.ipt.siga.entity.Course;
import cl.ipt.siga.entity.CourseEnrollment;
import cl.ipt.siga.entity.Student;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import cl.ipt.siga.entity.enums.CourseEnrollmentStatus;
import cl.ipt.siga.entity.enums.StudentStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.AcademicPeriodRepository;
import cl.ipt.siga.repository.CourseEnrollmentRepository;
import cl.ipt.siga.repository.CourseRepository;
import cl.ipt.siga.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    public AcademicPeriodService(
            AcademicPeriodRepository academicPeriodRepository,
            CourseEnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            StudentRepository studentRepository
    ) {
        this.academicPeriodRepository = academicPeriodRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<AcademicPeriodResponse> findAll() {
        return academicPeriodRepository.findAllByOrderByYearDescSemesterDesc().stream()
                .map(AcademicPeriodResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcademicPeriodResponse findById(Long id) {
        return AcademicPeriodResponse.from(getPeriod(id));
    }

    public AcademicPeriodResponse create(AcademicPeriodRequest request) {
        if (academicPeriodRepository.findByYearAndSemester(request.year(), request.semester()).isPresent()) {
            throw new BusinessConflictException("An academic period with this year and semester already exists.");
        }
        return AcademicPeriodResponse.from(
                academicPeriodRepository.save(new AcademicPeriod(request.year(), request.semester()))
        );
    }

    public AcademicPeriodResponse updateStatus(Long id, AcademicPeriodStatus status) {
        AcademicPeriod period = academicPeriodRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + id));
        if (period.getStatus() == AcademicPeriodStatus.CLOSED) {
            if (status == AcademicPeriodStatus.CLOSED) {
                return AcademicPeriodResponse.from(period);
            }
            throw new BusinessConflictException("A closed academic period cannot be reopened.");
        }
        if (status == AcademicPeriodStatus.OPEN) {
            return AcademicPeriodResponse.from(period);
        }

        closePeriod(period);
        return AcademicPeriodResponse.from(academicPeriodRepository.save(period));
    }

    private void closePeriod(AcademicPeriod period) {
        List<CourseEnrollment> enrollments = enrollmentRepository.findByAcademicPeriodIdAndStatusWithStudent(
                period.getId(),
                CourseEnrollmentStatus.ENROLLED
        );
        List<String> incompleteSections = enrollments.stream()
                .filter(enrollment -> enrollment.getFinalGrade() == null)
                .map(enrollment -> enrollment.getSection().getId() + " (" + enrollment.getCourse().getCode() + ")")
                .distinct()
                .sorted()
                .toList();
        if (!incompleteSections.isEmpty()) {
            throw new BusinessConflictException(
                    "Final grades are missing for sections: " + String.join(", ", incompleteSections)
            );
        }

        enrollments.stream()
                .map(enrollment -> enrollment.getCourse().getId())
                .distinct()
                .sorted()
                .forEach(courseId -> courseRepository.findByIdForUpdate(courseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId)));

        period.setStatus(AcademicPeriodStatus.CLOSED);
        academicPeriodRepository.saveAndFlush(period);

        Map<Long, Student> students = new LinkedHashMap<>();
        for (CourseEnrollment enrollment : enrollments) {
            students.putIfAbsent(enrollment.getStudent().getId(), enrollment.getStudent());
        }
        for (Student student : students.values()) {
            updateAcademicStatus(student, enrollments);
        }
    }

    private void updateAcademicStatus(Student student, List<CourseEnrollment> periodEnrollments) {
        if (student.getStatus() == StudentStatus.GRADUATED || student.getStatus() == StudentStatus.DISMISSED) {
            return;
        }

        List<CourseEnrollment> studentEnrollments = periodEnrollments.stream()
                .filter(enrollment -> enrollment.getStudent().getId().equals(student.getId()))
                .toList();
        boolean failedTwice = studentEnrollments.stream()
                .filter(enrollment -> enrollment.getFinalGrade().compareTo(BigDecimal.valueOf(4)) < 0)
                .anyMatch(enrollment -> enrollmentRepository.countFailedAttempts(
                        student.getId(),
                        enrollment.getCourse().getId(),
                        BigDecimal.valueOf(4)
                ) >= 2);
        if (failedTwice) {
            student.setStatus(StudentStatus.DISMISSED);
            studentRepository.save(student);
            return;
        }

        Set<Long> passedCourseIds = enrollmentRepository.findPassedCourseIdsByStudentId(student.getId());
        List<Course> planCourses = courseRepository.findByStudyPlanIdOrderBySemesterAscCodeAsc(
                student.getStudyPlan().getId()
        );
        if (!planCourses.isEmpty() && planCourses.stream().allMatch(course -> passedCourseIds.contains(course.getId()))) {
            student.setStatus(StudentStatus.GRADUATED);
            studentRepository.save(student);
        }
    }

    private AcademicPeriod getPeriod(Long id) {
        return academicPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + id));
    }
}
