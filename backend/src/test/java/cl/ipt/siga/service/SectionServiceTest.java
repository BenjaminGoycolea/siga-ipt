package cl.ipt.siga.service;

import cl.ipt.siga.dto.SectionRequest;
import cl.ipt.siga.entity.AcademicPeriod;
import cl.ipt.siga.entity.Career;
import cl.ipt.siga.entity.Course;
import cl.ipt.siga.entity.Section;
import cl.ipt.siga.entity.StudyPlan;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private SectionService sectionService;

    @Test
    void findByPeriodReturnsSections() {
        AcademicPeriod period = period(1L, AcademicPeriodStatus.OPEN);
        Section section = section(1L, period, course(1L), teacher(1L));
        section.setScheduleBlocks(new java.util.LinkedHashSet<>(List.of("L1", "W2", "V3")));
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(sectionRepository.findByAcademicPeriodIdOrderByCourseCodeAsc(1L)).thenReturn(List.of(section));

        assertThat(sectionService.findByPeriod(1L)).hasSize(1);
    }

    @Test
    void findByPeriodRejectsMissingPeriod() {
        when(academicPeriodRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.findByPeriod(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPersistsSectionWithValidatedSchedule() {
        AcademicPeriod period = period(1L, AcademicPeriodStatus.OPEN);
        Course course = course(2L);
        Teacher teacher = teacher(3L);
        when(academicPeriodRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(period));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(teacher));
        when(sectionRepository.findByAcademicPeriodIdAndCourseId(1L, 2L)).thenReturn(Optional.empty());
        when(sectionRepository.findByTeacherIdAndAcademicPeriodId(3L, 1L)).thenReturn(List.of());
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> {
            Section saved = invocation.getArgument(0);
            saved.setId(4L);
            return saved;
        });

        var result = sectionService.create(1L, request(2L, 3L, 40, List.of("l1", "W2", "V3")));

        assertThat(result.id()).isEqualTo(4L);
        assertThat(result.capacity()).isEqualTo(40);
        assertThat(result.scheduleBlocks()).containsExactly("L1", "V3", "W2");
    }

    @Test
    void createRejectsClosedPeriod() {
        when(academicPeriodRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(period(1L, AcademicPeriodStatus.CLOSED)));

        assertThatThrownBy(() -> sectionService.create(1L, request(2L, 3L, 40, List.of("L1", "W2", "V3"))))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("open period");
    }

    @Test
    void createRejectsMissingCourse() {
        when(academicPeriodRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(period(1L, AcademicPeriodStatus.OPEN)));
        when(courseRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.create(1L, request(2L, 3L, 40, List.of("L1", "W2", "V3"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRejectsInactiveTeacher() {
        Teacher teacher = teacher(3L);
        teacher.setStatus(TeacherStatus.INACTIVE);
        when(academicPeriodRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(period(1L, AcademicPeriodStatus.OPEN)));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course(2L)));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> sectionService.create(1L, request(2L, 3L, 40, List.of("L1", "W2", "V3"))))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("active teachers");
    }

    @Test
    void createRejectsSecondSectionForSameCourseAndPeriod() {
        AcademicPeriod period = period(1L, AcademicPeriodStatus.OPEN);
        when(academicPeriodRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(period));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course(2L)));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(teacher(3L)));
        when(sectionRepository.findByAcademicPeriodIdAndCourseId(1L, 2L))
                .thenReturn(Optional.of(section(5L, period, course(2L), teacher(4L))));

        assertThatThrownBy(() -> sectionService.create(1L, request(2L, 3L, 40, List.of("L1", "W2", "V3"))))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("only one section");
    }

    @Test
    void createRejectsCapacityOverSixty() {
        when(academicPeriodRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(period(1L, AcademicPeriodStatus.OPEN)));

        assertThatThrownBy(() -> sectionService.create(1L, request(2L, 3L, 61, List.of("L1", "W2", "V3"))))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void createRejectsMalformedScheduleBlock() {
        when(academicPeriodRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(period(1L, AcademicPeriodStatus.OPEN)));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course(2L)));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(teacher(3L)));
        when(sectionRepository.findByAcademicPeriodIdAndCourseId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.create(1L, request(2L, 3L, 40, List.of("X7", "W2", "V3"))))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("valid weekday");
    }

    @Test
    void createRejectsDuplicateScheduleBlocks() {
        when(academicPeriodRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(period(1L, AcademicPeriodStatus.OPEN)));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course(2L)));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(teacher(3L)));
        when(sectionRepository.findByAcademicPeriodIdAndCourseId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.create(1L, request(2L, 3L, 40, List.of("L1", "L1", "W2"))))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("duplicates");
    }

    @Test
    void createRejectsScheduleWithWrongNumberOfBlocks() {
        when(academicPeriodRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(period(1L, AcademicPeriodStatus.OPEN)));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course(2L)));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(teacher(3L)));
        when(sectionRepository.findByAcademicPeriodIdAndCourseId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.create(1L, request(2L, 3L, 40, List.of("L1", "W2"))))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("TEL hours");
    }

    @Test
    void createRejectsTeacherScheduleConflict() {
        AcademicPeriod period = period(1L, AcademicPeriodStatus.OPEN);
        Section existing = section(5L, period, course(6L), teacher(3L));
        existing.setScheduleBlocks(new java.util.LinkedHashSet<>(List.of("L1", "M2", "W3")));
        when(academicPeriodRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(period));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course(2L)));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(teacher(3L)));
        when(sectionRepository.findByAcademicPeriodIdAndCourseId(1L, 2L)).thenReturn(Optional.empty());
        when(sectionRepository.findByTeacherIdAndAcademicPeriodId(3L, 1L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> sectionService.create(1L, request(2L, 3L, 40, List.of("L1", "W2", "V3"))))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("already has a section");
    }

    @Test
    void updateRejectsMissingSection() {
        when(sectionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.update(404L, request(2L, 3L, 40, List.of("L1", "W2", "V3"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCannotLowerCapacityBelowCurrentEnrollments() {
        AcademicPeriod period = period(1L, AcademicPeriodStatus.OPEN);
        Course course = course(2L);
        Teacher teacher = teacher(3L);
        Section section = section(4L, period, course, teacher);
        section.setScheduleBlocks(new java.util.LinkedHashSet<>(List.of("L1", "W2", "V3")));
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section));
        when(sectionRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(section));
        when(academicPeriodRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(period));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(teacher));
        when(sectionRepository.findByAcademicPeriodIdAndCourseId(1L, 2L)).thenReturn(Optional.of(section));
        when(sectionRepository.findByTeacherIdAndAcademicPeriodId(3L, 1L)).thenReturn(List.of(section));
        when(enrollmentRepository.countBySectionIdAndStatus(4L, CourseEnrollmentStatus.ENROLLED)).thenReturn(5);

        assertThatThrownBy(() -> sectionService.update(4L, request(2L, 3L, 4, List.of("L1", "W2", "V3"))))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("current enrollment");
    }

    private SectionRequest request(Long courseId, Long teacherId, int capacity, List<String> blocks) {
        return new SectionRequest(courseId, teacherId, capacity, blocks);
    }

    private AcademicPeriod period(Long id, AcademicPeriodStatus status) {
        AcademicPeriod period = new AcademicPeriod(2027, 1);
        period.setId(id);
        period.setStatus(status);
        return period;
    }

    private Course course(Long id) {
        Career career = new Career("TEST", "Test career", "Description", 2020, 40);
        career.setId(1L);
        StudyPlan plan = new StudyPlan("1", true, career);
        plan.setId(1L);
        Course course = new Course("TST101", "Test course", 1, 2, 2, 2, 5, plan);
        course.setId(id);
        return course;
    }

    private Teacher teacher(Long id) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setNationalId("RUN-" + id);
        teacher.setFullName("Teacher " + id);
        teacher.setEmail("teacher" + id + "@example.edu");
        teacher.setProfessionalTitle("Engineer");
        teacher.setAcademicDegree("Master");
        teacher.setStatus(TeacherStatus.ACTIVE);
        return teacher;
    }

    private Section section(Long id, AcademicPeriod period, Course course, Teacher teacher) {
        Section section = new Section();
        section.setId(id);
        section.setAcademicPeriod(period);
        section.setCourse(course);
        section.setTeacher(teacher);
        section.setCapacity(40);
        return section;
    }
}
