package cl.ipt.siga.service;

import cl.ipt.siga.dto.TeacherRequest;
import cl.ipt.siga.entity.Teacher;
import cl.ipt.siga.entity.enums.TeacherStatus;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.TeacherRepository;
import cl.ipt.siga.repository.SectionRepository;
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
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private TeacherService teacherService;

    @Test
    void findAllReturnsTeachersSortedByName() {
        when(teacherRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(teacher(1L, "RUN-1")));

        assertThat(teacherService.findAll(null)).hasSize(1);
        verify(teacherRepository).findAllByOrderByFullNameAsc();
    }

    @Test
    void findAllCanFilterByStatus() {
        when(teacherRepository.findByStatusOrderByFullNameAsc(TeacherStatus.ACTIVE))
                .thenReturn(List.of(teacher(1L, "RUN-1")));

        assertThat(teacherService.findAll(TeacherStatus.ACTIVE))
                .extracting(response -> response.status())
                .containsExactly(TeacherStatus.ACTIVE);
    }

    @Test
    void findByIdReturnsTeacher() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, "RUN-1")));

        assertThat(teacherService.findById(1L).nationalId()).isEqualTo("RUN-1");
    }

    @Test
    void findByIdRejectsMissingTeacher() {
        when(teacherRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createNormalizesFieldsAndDefaultsActiveStatus() {
        when(teacherRepository.findByNationalId("12.345.678-K")).thenReturn(Optional.empty());
        when(teacherRepository.findByEmailIgnoreCase("teacher@example.edu")).thenReturn(Optional.empty());
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> {
            Teacher saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        var result = teacherService.create(request(
                " 12.345.678-k ", " Teacher Name ", " Teacher@Example.edu ", " Engineer ", " Master "
        ));

        assertThat(result.nationalId()).isEqualTo("12.345.678-K");
        assertThat(result.fullName()).isEqualTo("Teacher Name");
        assertThat(result.email()).isEqualTo("teacher@example.edu");
        assertThat(result.professionalTitle()).isEqualTo("Engineer");
        assertThat(result.academicDegree()).isEqualTo("Master");
        assertThat(result.status()).isEqualTo(TeacherStatus.ACTIVE);
    }

    @Test
    void createRejectsDuplicateNationalId() {
        when(teacherRepository.findByNationalId("RUN-1")).thenReturn(Optional.of(teacher(1L, "RUN-1")));

        assertThatThrownBy(() -> teacherService.create(request(
                "RUN-1", "Name", "new@example.edu", "Title", "Degree"
        ))).isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void createRejectsDuplicateEmailCaseInsensitively() {
        when(teacherRepository.findByNationalId("RUN-2")).thenReturn(Optional.empty());
        when(teacherRepository.findByEmailIgnoreCase("teacher@example.edu"))
                .thenReturn(Optional.of(teacher(1L, "RUN-1")));

        assertThatThrownBy(() -> teacherService.create(request(
                "RUN-2", "Name", " Teacher@Example.edu ", "Title", "Degree"
        ))).isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void updateAllowsExistingIdentityAndPreservesStatus() {
        Teacher existing = teacher(1L, "RUN-1");
        existing.setStatus(TeacherStatus.INACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.findByNationalId("RUN-1")).thenReturn(Optional.of(existing));
        when(teacherRepository.findByEmailIgnoreCase("teacher@example.edu")).thenReturn(Optional.of(existing));
        when(teacherRepository.save(existing)).thenReturn(existing);

        var result = teacherService.update(1L, request(
                "RUN-1", "Updated Name", "teacher@example.edu", "Updated title", "Updated degree"
        ));

        assertThat(result.fullName()).isEqualTo("Updated Name");
        assertThat(result.status()).isEqualTo(TeacherStatus.INACTIVE);
    }

    @Test
    void updateRejectsIdentityUsedByAnotherTeacher() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, "RUN-1")));
        when(teacherRepository.findByNationalId("RUN-2")).thenReturn(Optional.of(teacher(2L, "RUN-2")));

        assertThatThrownBy(() -> teacherService.update(1L, request(
                "RUN-2", "Updated", "new@example.edu", "Title", "Degree"
        ))).isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void updateRejectsMissingTeacher() {
        when(teacherRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.update(404L, request(
                "RUN", "Name", "email@example.edu", "Title", "Degree"
        ))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatusChangesAndSavesTeacher() {
        Teacher existing = teacher(1L, "RUN-1");
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sectionRepository.existsByTeacherIdAndAcademicPeriod_Status(1L, AcademicPeriodStatus.OPEN))
                .thenReturn(false);
        when(teacherRepository.save(existing)).thenReturn(existing);

        var result = teacherService.updateStatus(1L, TeacherStatus.INACTIVE);

        assertThat(result.status()).isEqualTo(TeacherStatus.INACTIVE);
        verify(teacherRepository).save(existing);
    }

    @Test
    void updateStatusRejectsDeactivatingTeacherAssignedToOpenPeriodSection() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, "RUN-1")));
        when(sectionRepository.existsByTeacherIdAndAcademicPeriod_Status(1L, AcademicPeriodStatus.OPEN))
                .thenReturn(true);

        assertThatThrownBy(() -> teacherService.updateStatus(1L, TeacherStatus.INACTIVE))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("open academic period");
    }

    @Test
    void updateStatusRejectsMissingTeacher() {
        when(teacherRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.updateStatus(404L, TeacherStatus.INACTIVE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateKeycloakSubjectLinksTeacherWithoutExposingSubject() {
        Teacher existing = teacher(1L, "RUN-1");
        when(teacherRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.findByKeycloakSubject("keycloak-subject")).thenReturn(Optional.empty());
        when(teacherRepository.save(existing)).thenReturn(existing);

        var result = teacherService.updateKeycloakSubject(1L, " keycloak-subject ");

        assertThat(existing.getKeycloakSubject()).isEqualTo("keycloak-subject");
        assertThat(result.identityLinked()).isTrue();
    }

    @Test
    void updateKeycloakSubjectRejectsAccountAlreadyLinkedToAnotherTeacher() {
        Teacher existing = teacher(1L, "RUN-1");
        when(teacherRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.findByKeycloakSubject("keycloak-subject"))
                .thenReturn(Optional.of(teacher(2L, "RUN-2")));

        assertThatThrownBy(() -> teacherService.updateKeycloakSubject(1L, "keycloak-subject"))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("already linked");
    }

    private TeacherRequest request(
            String nationalId,
            String fullName,
            String email,
            String professionalTitle,
            String academicDegree
    ) {
        return new TeacherRequest(nationalId, fullName, email, professionalTitle, academicDegree);
    }

    private Teacher teacher(Long id, String nationalId) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setNationalId(nationalId);
        teacher.setFullName("Teacher " + nationalId);
        teacher.setEmail("teacher@example.edu");
        teacher.setProfessionalTitle("Engineer");
        teacher.setAcademicDegree("Master");
        teacher.setStatus(TeacherStatus.ACTIVE);
        return teacher;
    }
}
