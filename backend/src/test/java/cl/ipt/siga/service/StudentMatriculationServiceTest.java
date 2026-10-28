package cl.ipt.siga.service;

import cl.ipt.siga.entity.AcademicPeriod;
import cl.ipt.siga.entity.Student;
import cl.ipt.siga.entity.StudentMatriculation;
import cl.ipt.siga.entity.enums.CourseEnrollmentStatus;
import cl.ipt.siga.entity.enums.StudentStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.AcademicPeriodRepository;
import cl.ipt.siga.repository.CourseEnrollmentRepository;
import cl.ipt.siga.repository.StudentMatriculationRepository;
import cl.ipt.siga.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentMatriculationServiceTest {

    @Mock
    private StudentMatriculationRepository matriculationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private StudentMatriculationService matriculationService;

    @Test
    void updateCreatesMatriculationForRegularStudent() {
        Student student = student(1L, StudentStatus.REGULAR);
        AcademicPeriod period = period(2L);
        when(studentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(student));
        when(academicPeriodRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(period));
        when(matriculationRepository.findByStudentIdAndAcademicPeriodId(1L, 2L)).thenReturn(Optional.empty());
        when(matriculationRepository.save(any(StudentMatriculation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = matriculationService.update(1L, 2L, true);

        assertThat(result.studentId()).isEqualTo(1L);
        assertThat(result.academicPeriodCode()).isEqualTo("2027-1");
        assertThat(result.matriculated()).isTrue();
    }

    @Test
    void updateChangesExistingMatriculation() {
        Student student = student(1L, StudentStatus.REGULAR);
        AcademicPeriod period = period(2L);
        StudentMatriculation existing = new StudentMatriculation(student, period, true);
        when(studentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(student));
        when(academicPeriodRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(period));
        when(enrollmentRepository.existsByStudentIdAndAcademicPeriodIdAndStatus(
                1L, 2L, CourseEnrollmentStatus.ENROLLED
        )).thenReturn(false);
        when(matriculationRepository.findByStudentIdAndAcademicPeriodId(1L, 2L)).thenReturn(Optional.of(existing));
        when(matriculationRepository.save(existing)).thenReturn(existing);

        var result = matriculationService.update(1L, 2L, false);

        assertThat(result.matriculated()).isFalse();
    }

    @Test
    void updateRejectsMissingStudent() {
        when(studentRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matriculationService.update(404L, 2L, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void updateRejectsMissingPeriod() {
        when(studentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(student(1L, StudentStatus.REGULAR)));
        when(academicPeriodRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matriculationService.update(1L, 404L, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Academic period not found");
    }

    @Test
    void updateRejectsMatriculationForNonRegularStudent() {
        when(studentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(student(1L, StudentStatus.DISMISSED)));
        when(academicPeriodRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(period(2L)));

        assertThatThrownBy(() -> matriculationService.update(1L, 2L, true))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("Only regular students");
    }

    @Test
    void updateCannotRevokeMatriculationWhileStudentHasEnrollments() {
        when(studentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(student(1L, StudentStatus.REGULAR)));
        when(academicPeriodRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(period(2L)));
        when(enrollmentRepository.existsByStudentIdAndAcademicPeriodIdAndStatus(
                1L, 2L, CourseEnrollmentStatus.ENROLLED
        )).thenReturn(true);

        assertThatThrownBy(() -> matriculationService.update(1L, 2L, false))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("cannot be revoked");
    }

    private Student student(Long id, StudentStatus status) {
        Student student = new Student();
        student.setId(id);
        student.setStatus(status);
        return student;
    }

    private AcademicPeriod period(Long id) {
        AcademicPeriod period = new AcademicPeriod(2027, 1);
        period.setId(id);
        return period;
    }
}
