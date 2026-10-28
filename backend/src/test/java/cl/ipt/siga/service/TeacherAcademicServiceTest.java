package cl.ipt.siga.service;

import cl.ipt.siga.entity.AcademicPeriod;
import cl.ipt.siga.entity.Section;
import cl.ipt.siga.entity.Teacher;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import cl.ipt.siga.entity.enums.TeacherStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.repository.AcademicPeriodRepository;
import cl.ipt.siga.repository.CourseEnrollmentRepository;
import cl.ipt.siga.repository.SectionRepository;
import cl.ipt.siga.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherAcademicServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private TeacherAcademicService teacherAcademicService;

    @Test
    void findAssignedSectionsReturnsOnlySectionsOwnedByTeacher() {
        Teacher teacher = teacher(1L, TeacherStatus.ACTIVE);
        Section section = new Section();
        section.setId(3L);
        section.setTeacher(teacher);
        section.setAcademicPeriod(new AcademicPeriod(2027, 1));
        section.setCourse(new cl.ipt.siga.entity.Course(
                "TAP101", "Programming", 1, 2, 0, 0, 3, null
        ));
        when(teacherRepository.findByKeycloakSubject("subject")).thenReturn(Optional.of(teacher));
        when(sectionRepository.findByTeacherIdAndAcademicPeriodId(1L, 2L)).thenReturn(List.of(section));

        assertThat(teacherAcademicService.findAssignedSections("subject", 2L))
                .extracting(response -> response.id())
                .containsExactly(3L);
    }

    @Test
    void findAssignedSectionsRejectsMissingSubject() {
        assertThatThrownBy(() -> teacherAcademicService.findAssignedSections(" ", 2L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("no Keycloak subject");
    }

    @Test
    void findAssignedSectionsRejectsUnlinkedAccount() {
        when(teacherRepository.findByKeycloakSubject("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherAcademicService.findAssignedSections("unknown", 2L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("not linked to a teacher");
    }

    @Test
    void findAssignedSectionsRejectsInactiveTeacher() {
        when(teacherRepository.findByKeycloakSubject("subject"))
                .thenReturn(Optional.of(teacher(1L, TeacherStatus.INACTIVE)));

        assertThatThrownBy(() -> teacherAcademicService.findAssignedSections("subject", 2L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("active teachers");
    }

    private Teacher teacher(Long id, TeacherStatus status) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setStatus(status);
        teacher.setFullName("Teacher " + id);
        return teacher;
    }
}
