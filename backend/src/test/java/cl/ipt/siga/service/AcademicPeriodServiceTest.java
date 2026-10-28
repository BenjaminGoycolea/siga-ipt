package cl.ipt.siga.service;

import cl.ipt.siga.dto.AcademicPeriodRequest;
import cl.ipt.siga.entity.AcademicPeriod;
import cl.ipt.siga.entity.Course;
import cl.ipt.siga.entity.CourseEnrollment;
import cl.ipt.siga.entity.Section;
import cl.ipt.siga.entity.Student;
import cl.ipt.siga.entity.StudyPlan;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import cl.ipt.siga.entity.enums.CourseEnrollmentStatus;
import cl.ipt.siga.entity.enums.StudentStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.AcademicPeriodRepository;
import cl.ipt.siga.repository.CourseEnrollmentRepository;
import cl.ipt.siga.repository.CourseRepository;
import cl.ipt.siga.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicPeriodServiceTest {

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private AcademicPeriodService academicPeriodService;

    @Test
    void findAllReturnsPeriodsInRepositoryOrder() {
        when(academicPeriodRepository.findAllByOrderByYearDescSemesterDesc())
                .thenReturn(List.of(new AcademicPeriod(2027, 1)));

        assertThat(academicPeriodService.findAll())
                .extracting(period -> period.code())
                .containsExactly("2027-1");
    }

    @Test
    void findByIdReturnsPeriod() {
        AcademicPeriod period = period(1L, 2027, 2);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));

        assertThat(academicPeriodService.findById(1L).code()).isEqualTo("2027-2");
    }

    @Test
    void findByIdRejectsMissingPeriod() {
        when(academicPeriodRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicPeriodService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createDefaultsPeriodToOpen() {
        when(academicPeriodRepository.findByYearAndSemester(2027, 1)).thenReturn(Optional.empty());
        when(academicPeriodRepository.save(any(AcademicPeriod.class))).thenAnswer(invocation -> {
            AcademicPeriod period = invocation.getArgument(0);
            period.setId(5L);
            return period;
        });

        var result = academicPeriodService.create(new AcademicPeriodRequest(2027, 1));

        assertThat(result.code()).isEqualTo("2027-1");
        assertThat(result.status()).isEqualTo(AcademicPeriodStatus.OPEN);
    }

    @Test
    void createRejectsDuplicateYearAndSemester() {
        when(academicPeriodRepository.findByYearAndSemester(2027, 1))
                .thenReturn(Optional.of(period(1L, 2027, 1)));

        assertThatThrownBy(() -> academicPeriodService.create(new AcademicPeriodRequest(2027, 1)))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void updateStatusChangesPeriodState() {
        AcademicPeriod period = period(1L, 2027, 1);
        when(academicPeriodRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriodIdAndStatusWithStudent(
                1L, CourseEnrollmentStatus.ENROLLED
        )).thenReturn(List.of());
        when(academicPeriodRepository.save(period)).thenReturn(period);

        assertThat(academicPeriodService.updateStatus(1L, AcademicPeriodStatus.CLOSED).status())
                .isEqualTo(AcademicPeriodStatus.CLOSED);
    }

    @Test
    void updateStatusRejectsMissingPeriod() {
        when(academicPeriodRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicPeriodService.updateStatus(404L, AcademicPeriodStatus.CLOSED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatusRejectsClosingWhenSectionHasMissingGrades() {
        AcademicPeriod period = period(1L, 2027, 1);
        Course course = course(10L, "TAP101", null);
        Section section = new Section();
        section.setId(5L);
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourse(course);
        enrollment.setSection(section);
        when(academicPeriodRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriodIdAndStatusWithStudent(
                1L, CourseEnrollmentStatus.ENROLLED
        )).thenReturn(List.of(enrollment));

        assertThatThrownBy(() -> academicPeriodService.updateStatus(1L, AcademicPeriodStatus.CLOSED))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("5 (TAP101)");
        assertThat(period.getStatus()).isEqualTo(AcademicPeriodStatus.OPEN);
    }

    @Test
    void closingPeriodDismissesStudentAfterSecondFailureInSameCourse() {
        AcademicPeriod period = period(1L, 2027, 1);
        StudyPlan plan = new StudyPlan();
        plan.setId(7L);
        Student student = student(2L, plan);
        Course course = course(10L, "TAP101", plan);
        CourseEnrollment enrollment = enrollment(student, course, period, 3.9);
        when(academicPeriodRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriodIdAndStatusWithStudent(
                1L, CourseEnrollmentStatus.ENROLLED
        )).thenReturn(List.of(enrollment));
        when(enrollmentRepository.countFailedAttempts(2L, 10L, java.math.BigDecimal.valueOf(4))).thenReturn(2L);
        when(courseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(course));
        when(academicPeriodRepository.save(period)).thenReturn(period);

        academicPeriodService.updateStatus(1L, AcademicPeriodStatus.CLOSED);

        assertThat(period.getStatus()).isEqualTo(AcademicPeriodStatus.CLOSED);
        assertThat(student.getStatus()).isEqualTo(StudentStatus.DISMISSED);
        org.mockito.Mockito.verify(studentRepository).save(student);
    }

    @Test
    void closingPeriodGraduatesStudentWhoPassedEveryCourseInPlan() {
        AcademicPeriod period = period(1L, 2027, 1);
        StudyPlan plan = new StudyPlan();
        plan.setId(7L);
        Student student = student(2L, plan);
        Course currentCourse = course(10L, "TAP101", plan);
        Course priorCourse = course(11L, "TAP102", plan);
        CourseEnrollment enrollment = enrollment(student, currentCourse, period, 4.0);
        when(academicPeriodRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriodIdAndStatusWithStudent(
                1L, CourseEnrollmentStatus.ENROLLED
        )).thenReturn(List.of(enrollment));
        when(courseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(currentCourse));
        when(enrollmentRepository.findPassedCourseIdsByStudentId(2L)).thenReturn(Set.of(10L, 11L));
        when(courseRepository.findByStudyPlanIdOrderBySemesterAscCodeAsc(7L))
                .thenReturn(List.of(currentCourse, priorCourse));
        when(academicPeriodRepository.save(period)).thenReturn(period);

        academicPeriodService.updateStatus(1L, AcademicPeriodStatus.CLOSED);

        assertThat(student.getStatus()).isEqualTo(StudentStatus.GRADUATED);
        org.mockito.Mockito.verify(studentRepository).save(student);
    }

    @Test
    void closedPeriodCannotBeReopened() {
        AcademicPeriod period = period(1L, 2027, 1);
        period.setStatus(AcademicPeriodStatus.CLOSED);
        when(academicPeriodRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> academicPeriodService.updateStatus(1L, AcademicPeriodStatus.OPEN))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("cannot be reopened");
    }

    private Student student(Long id, StudyPlan plan) {
        Student student = new Student();
        student.setId(id);
        student.setStudyPlan(plan);
        return student;
    }

    private Course course(Long id, String code, StudyPlan plan) {
        Course course = new Course();
        course.setId(id);
        course.setCode(code);
        course.setStudyPlan(plan);
        return course;
    }

    private CourseEnrollment enrollment(Student student, Course course, AcademicPeriod period, double grade) {
        CourseEnrollment enrollment = new CourseEnrollment(student, course, new Section(), period);
        enrollment.setFinalGrade(java.math.BigDecimal.valueOf(grade));
        return enrollment;
    }

    private AcademicPeriod period(Long id, int year, int semester) {
        AcademicPeriod period = new AcademicPeriod(year, semester);
        period.setId(id);
        return period;
    }
}
