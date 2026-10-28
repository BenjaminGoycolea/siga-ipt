package cl.ipt.siga.service;

import cl.ipt.siga.dto.StudentRequest;
import cl.ipt.siga.entity.Career;
import cl.ipt.siga.entity.Student;
import cl.ipt.siga.entity.StudyPlan;
import cl.ipt.siga.entity.enums.CareerStatus;
import cl.ipt.siga.entity.enums.StudentStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CareerRepository;
import cl.ipt.siga.repository.StudentRepository;
import cl.ipt.siga.repository.StudyPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @InjectMocks
    private StudentService studentService;

    private Career career;
    private StudyPlan plan;

    @BeforeEach
    void setUp() {
        career = new Career("10450", "Programming", "Programming career", 2000, 50);
        career.setId(10L);
        plan = new StudyPlan("2021.3", true, career);
        plan.setId(20L);
    }

    @Test
    void createNormalizesIdentityAndUsesCurrentPlan() {
        when(studentRepository.findByNationalId("12.345.678-K")).thenReturn(Optional.empty());
        when(studentRepository.findByEmailIgnoreCase("student@example.edu")).thenReturn(Optional.empty());
        when(careerRepository.findById(10L)).thenReturn(Optional.of(career));
        when(studyPlanRepository.findByCareerIdAndCurrentPlanTrue(10L)).thenReturn(Optional.of(plan));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setId(1L);
            return student;
        });

        var result = studentService.create(new StudentRequest(
                " 12.345.678-k ",
                " Student Name ",
                " Student@Example.edu ",
                10L
        ));

        assertThat(result.nationalId()).isEqualTo("12.345.678-K");
        assertThat(result.fullName()).isEqualTo("Student Name");
        assertThat(result.email()).isEqualTo("student@example.edu");
        assertThat(result.status()).isEqualTo(StudentStatus.REGULAR);
        assertThat(result.studyPlanVersion()).isEqualTo("2021.3");
    }

    @Test
    void createRejectsDuplicateNationalId() {
        when(studentRepository.findByNationalId("RUN-1")).thenReturn(Optional.of(student(2L)));

        assertThatThrownBy(() -> studentService.create(request("RUN-1", "other@example.edu", 10L)))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("national ID");
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(studentRepository.findByNationalId("RUN-1")).thenReturn(Optional.empty());
        when(studentRepository.findByEmailIgnoreCase("student@example.edu")).thenReturn(Optional.of(student(2L)));

        assertThatThrownBy(() -> studentService.create(request("RUN-1", "student@example.edu", 10L)))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("email");
    }

    @Test
    void createRejectsUnknownCareer() {
        allowUniqueIdentity("RUN-1", "student@example.edu");
        when(careerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.create(request("RUN-1", "student@example.edu", 99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Career not found");
    }

    @Test
    void createRejectsInactiveCareer() {
        career.setStatus(CareerStatus.INACTIVE);
        allowUniqueIdentity("RUN-1", "student@example.edu");
        when(careerRepository.findById(10L)).thenReturn(Optional.of(career));

        assertThatThrownBy(() -> studentService.create(request("RUN-1", "student@example.edu", 10L)))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("inactive career");
    }

    @Test
    void createRejectsCareerWithoutCurrentPlan() {
        allowUniqueIdentity("RUN-1", "student@example.edu");
        when(careerRepository.findById(10L)).thenReturn(Optional.of(career));
        when(studyPlanRepository.findByCareerIdAndCurrentPlanTrue(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.create(request("RUN-1", "student@example.edu", 10L)))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("no current study plan");
    }

    @Test
    void updateAllowsExistingStudentIdentityAndUpdatesPersonalDetails() {
        Student existing = student(5L);
        when(studentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(studentRepository.findByNationalId("RUN-1")).thenReturn(Optional.of(existing));
        when(studentRepository.findByEmailIgnoreCase("student@example.edu")).thenReturn(Optional.of(existing));
        when(careerRepository.findById(10L)).thenReturn(Optional.of(career));
        when(studentRepository.save(existing)).thenReturn(existing);

        var result = studentService.update(5L, new StudentRequest(
                "RUN-1",
                "Updated Name",
                "STUDENT@EXAMPLE.EDU",
                10L
        ));

        assertThat(result.fullName()).isEqualTo("Updated Name");
        assertThat(result.email()).isEqualTo("student@example.edu");
    }

    @Test
    void updateRejectsChangingCareerUntilHistoryExists() {
        Career otherCareer = new Career("10451", "Data", "Data career", 2018, 50);
        otherCareer.setId(11L);
        Student existing = student(5L);
        when(studentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(studentRepository.findByNationalId("RUN-1")).thenReturn(Optional.of(existing));
        when(studentRepository.findByEmailIgnoreCase("student@example.edu")).thenReturn(Optional.of(existing));
        when(careerRepository.findById(11L)).thenReturn(Optional.of(otherCareer));

        assertThatThrownBy(() -> studentService.update(
                5L,
                new StudentRequest("RUN-1", "Student", "student@example.edu", 11L)
        ))
                .isInstanceOf(BusinessConflictException.class)
        .hasMessageContaining("not supported");
    }

    @Test
    void updateRejectsDuplicateNationalIdOwnedByAnotherStudent() {
        when(studentRepository.findById(5L)).thenReturn(Optional.of(student(5L)));
        when(studentRepository.findByNationalId("RUN-2")).thenReturn(Optional.of(student(6L)));

        assertThatThrownBy(() -> studentService.update(
                5L,
                new StudentRequest("RUN-2", "Student", "student@example.edu", 10L)
        ))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("national ID");
    }

    @Test
    void updateRejectsDuplicateEmailOwnedByAnotherStudent() {
        when(studentRepository.findById(5L)).thenReturn(Optional.of(student(5L)));
        when(studentRepository.findByNationalId("RUN-1")).thenReturn(Optional.of(student(5L)));
        when(studentRepository.findByEmailIgnoreCase("other@example.edu")).thenReturn(Optional.of(student(6L)));

        assertThatThrownBy(() -> studentService.update(
                5L,
                new StudentRequest("RUN-1", "Student", "other@example.edu", 10L)
        ))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("email");
    }

    @Test
    void updateRejectsMissingStudent() {
        when(studentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.updateStatus(404L, StudentStatus.REGULAR))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void statusUpdateRejectsAutomaticallyAssignedStatuses() {
        when(studentRepository.findById(5L)).thenReturn(Optional.of(student(5L)));

        assertThatThrownBy(() -> studentService.updateStatus(5L, StudentStatus.GRADUATED))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("automatically");
        assertThatThrownBy(() -> studentService.updateStatus(5L, StudentStatus.DISMISSED))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("automatically");
    }

    @Test
    void statusUpdateAllowsAdministrativeStatuses() {
        Student existing = student(5L);
        when(studentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(existing)).thenReturn(existing);

        var result = studentService.updateStatus(5L, StudentStatus.POSTPONED);

        assertThat(result.status()).isEqualTo(StudentStatus.POSTPONED);
        verify(studentRepository).save(existing);
    }

    @Test
    void statusUpdateCannotChangeAutomaticallyAssignedTerminalStatus() {
        Student existing = student(5L);
        existing.setStatus(StudentStatus.GRADUATED);
        when(studentRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> studentService.updateStatus(5L, StudentStatus.REGULAR))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("cannot be changed");
    }

    @Test
    void findAllCanFilterByCareer() {
        Student existing = student(5L);
        when(studentRepository.findByCareerIdOrderByFullNameAsc(10L)).thenReturn(List.of(existing));

        assertThat(studentService.findAll(10L)).hasSize(1);
        verify(studentRepository).findByCareerIdOrderByFullNameAsc(10L);
    }

    private void allowUniqueIdentity(String nationalId, String email) {
        when(studentRepository.findByNationalId(nationalId)).thenReturn(Optional.empty());
        when(studentRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
    }

    private StudentRequest request(String nationalId, String email, Long careerId) {
        return new StudentRequest(nationalId, "Student Name", email, careerId);
    }

    private Student student(Long id) {
        Student student = new Student("RUN-1", "Student Name", "student@example.edu", career, plan);
        student.setId(id);
        return student;
    }
}
