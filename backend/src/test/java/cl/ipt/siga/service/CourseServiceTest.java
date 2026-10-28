package cl.ipt.siga.service;

import cl.ipt.siga.dto.CourseRequest;
import cl.ipt.siga.entity.Career;
import cl.ipt.siga.entity.Course;
import cl.ipt.siga.entity.StudyPlan;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CourseRepository;
import cl.ipt.siga.repository.CourseEnrollmentRepository;
import cl.ipt.siga.repository.StudyPlanRepository;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void findByStudyPlanReturnsCoursesInRepositoryOrder() {
        StudyPlan plan = plan(1L);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(courseRepository.findByStudyPlanIdOrderBySemesterAscCodeAsc(1L))
                .thenReturn(List.of(course(2L, "TAP101", 1, plan)));

        assertThat(courseService.findByStudyPlan(1L))
                .extracting(response -> response.code())
                .containsExactly("TAP101");
    }

    @Test
    void findByStudyPlanRejectsMissingPlan() {
        when(studyPlanRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findByStudyPlan(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createNormalizesCourseCodeAndAssociatesSamePlanPrerequisite() {
        StudyPlan plan = plan(1L);
        Course prerequisite = course(2L, "TAP101", 1, plan);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(courseRepository.findByStudyPlanIdAndCode(1L, "TAP201")).thenReturn(Optional.empty());
        when(courseRepository.findById(2L)).thenReturn(Optional.of(prerequisite));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        var result = courseService.create(1L, request(" tap201 ", 2, 2, 2, 2, 5, List.of(2L)));

        assertThat(result.code()).isEqualTo("TAP201");
        assertThat(result.prerequisiteCodes()).containsExactly("TAP101");
    }

    @Test
    void createRejectsDuplicateCodeWithinPlan() {
        StudyPlan plan = plan(1L);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(courseRepository.findByStudyPlanIdAndCode(1L, "TAP101"))
                .thenReturn(Optional.of(course(2L, "TAP101", 1, plan)));

        assertThatThrownBy(() -> courseService.create(1L, request("TAP101", 1, 2, 2, 2, 5, List.of())))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void createRejectsWeeklyTelHoursOfEightOrMore() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L)));
        when(courseRepository.findByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.create(1L, request("TAP101", 1, 2, 2, 4, 5, List.of())))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("TEL");
    }

    @Test
    void createRejectsPrerequisiteFromDifferentPlan() {
        StudyPlan plan = plan(1L);
        StudyPlan otherPlan = plan(2L);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(courseRepository.findByStudyPlanIdAndCode(1L, "TAP201")).thenReturn(Optional.empty());
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course(2L, "OTHER101", 1, otherPlan)));

        assertThatThrownBy(() -> courseService.create(1L, request("TAP201", 2, 2, 2, 2, 5, List.of(2L))))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("same study plan");
    }

    @Test
    void createRejectsPrerequisiteFromSameOrLaterSemester() {
        StudyPlan plan = plan(1L);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(courseRepository.findByStudyPlanIdAndCode(1L, "TAP201")).thenReturn(Optional.empty());
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course(2L, "TAP301", 3, plan)));

        assertThatThrownBy(() -> courseService.create(1L, request("TAP201", 2, 2, 2, 2, 5, List.of(2L))))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("earlier semester");
    }

    @Test
    void createRejectsDuplicatePrerequisiteIds() {
        StudyPlan plan = plan(1L);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(courseRepository.findByStudyPlanIdAndCode(1L, "TAP201")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.create(
                1L, request("TAP201", 2, 2, 2, 2, 5, List.of(2L, 2L))
        )).isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("duplicates");
    }

    @Test
    void updateRejectsMovingPrerequisiteToSameSemesterAsItsDependent() {
        StudyPlan plan = plan(1L);
        Course prerequisite = course(2L, "TAP101", 1, plan);
        Course dependent = course(3L, "TAP201", 2, plan);
        dependent.getPrerequisites().add(prerequisite);
        when(courseRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(prerequisite));
        when(courseRepository.findByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(Optional.of(prerequisite));
        when(courseRepository.findByPrerequisitesId(2L)).thenReturn(List.of(dependent));

        assertThatThrownBy(() -> courseService.update(2L, request("TAP101", 2, 2, 2, 2, 5, List.of())))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("depends on this prerequisite");
    }

    @Test
    void updateRejectsCourseAlreadyRecordedInAcademicHistory() {
        StudyPlan plan = plan(1L);
        Course course = course(2L, "TAP101", 1, plan);
        when(courseRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsInAcademicHistory(2L)).thenReturn(true);

        assertThatThrownBy(() -> courseService.update(
                2L, request("TAP101", 1, 2, 2, 2, 5, List.of())
        )).isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("academic history");
    }

    @Test
    void updateSavesCourseWhenDependentCoursesRemainValid() {
        StudyPlan plan = plan(1L);
        Course course = course(2L, "TAP101", 1, plan);
        when(courseRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsInAcademicHistory(2L)).thenReturn(false);
        when(courseRepository.findByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(Optional.of(course));
        when(courseRepository.findByPrerequisitesId(2L)).thenReturn(List.of());
        when(courseRepository.save(course)).thenReturn(course);

        var result = courseService.update(2L, request("tap101", 2, 1, 1, 1, 4, List.of()));

        assertThat(result.code()).isEqualTo("TAP101");
        assertThat(result.semester()).isEqualTo(2);
        assertThat(result.credits()).isEqualTo(4);
    }

    @Test
    void updateRejectsCourseAsItsOwnPrerequisite() {
        StudyPlan plan = plan(1L);
        Course course = course(2L, "TAP101", 1, plan);
        when(courseRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsInAcademicHistory(2L)).thenReturn(false);
        when(courseRepository.findByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(Optional.of(course));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.update(
        2L, request("TAP101", 2, 1, 1, 1, 4, List.of(2L))
        )).isInstanceOf(BusinessConflictException.class)
        .hasMessageContaining("own prerequisite");
    }

    @Test
    void createRejectsMoreThanThreePrerequisites() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L)));
        when(courseRepository.findByStudyPlanIdAndCode(1L, "TAP201")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.create(
        1L, request("TAP201", 2, 1, 1, 1, 4, List.of(1L, 2L, 3L, 4L))
        )).isInstanceOf(BusinessConflictException.class)
        .hasMessageContaining("more than three prerequisites");
    }

    @Test
    void createRejectsSemesterOutsideSupportedRange() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L)));
        when(courseRepository.findByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.create(
        1L, request("TAP101", 5, 1, 1, 1, 4, List.of())
        )).isInstanceOf(BusinessConflictException.class)
        .hasMessageContaining("semester");
    }

    @Test
    void findByIdRejectsMissingCourse() {
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CourseRequest request(
            String code,
            int semester,
            int theory,
            int exercise,
            int laboratory,
            int credits,
            List<Long> prerequisiteIds
    ) {
        return new CourseRequest(code, "Course " + code, semester, theory, exercise, laboratory, credits,
                prerequisiteIds);
    }

    private StudyPlan plan(Long id) {
        Career career = new Career("10450", "Programming", "Description", 2000, 50);
        career.setId(1L);
        StudyPlan plan = new StudyPlan("2021.3", true, career);
        plan.setId(id);
        return plan;
    }

    private Course course(Long id, String code, int semester, StudyPlan plan) {
        Course course = new Course(code, "Course " + code, semester, 2, 2, 2, 5, plan);
        course.setId(id);
        return course;
    }
}
