package cl.ipt.siga.service;

import cl.ipt.siga.dto.StudyPlanRequest;
import cl.ipt.siga.entity.Career;
import cl.ipt.siga.entity.StudyPlan;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CareerRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyPlanServiceTest {

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @InjectMocks
    private StudyPlanService studyPlanService;

    @Test
    void findByCareerReturnsPlansInRepositoryOrder() {
        Career career = career(1L);
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career));
        when(studyPlanRepository.findByCareerIdOrderByVersionDesc(1L))
                .thenReturn(List.of(plan(3L, "2022.3", true, career)));

        assertThat(studyPlanService.findByCareer(1L))
                .extracting(response -> response.version())
                .containsExactly("2022.3");
    }

    @Test
    void findByCareerRejectsMissingCareer() {
        when(careerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyPlanService.findByCareer(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createNormalizesVersionAndDoesNotMakePlanCurrentByDefault() {
        Career career = career(1L);
        when(careerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(career));
        when(studyPlanRepository.findByCareerIdAndVersion(1L, "2023.1")).thenReturn(Optional.empty());
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(invocation -> {
            StudyPlan saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        var result = studyPlanService.create(1L, new StudyPlanRequest(" 2023.1 ", false));

        assertThat(result.version()).isEqualTo("2023.1");
        assertThat(result.currentPlan()).isFalse();
    }

    @Test
    void createCurrentPlanDeactivatesPreviousCurrentPlan() {
        Career career = career(1L);
        StudyPlan current = plan(2L, "2022.3", true, career);
        when(careerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(career));
        when(studyPlanRepository.findByCareerIdAndVersion(1L, "2023.1")).thenReturn(Optional.empty());
        when(studyPlanRepository.findAllByCareerIdAndCurrentPlanTrue(1L)).thenReturn(List.of(current));
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(invocation -> {
            StudyPlan saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        var result = studyPlanService.create(1L, new StudyPlanRequest("2023.1", true));

        assertThat(result.currentPlan()).isTrue();
        assertThat(current.isCurrentPlan()).isFalse();
        verify(studyPlanRepository).save(any(StudyPlan.class));
    }

    @Test
    void createRejectsDuplicateVersionForCareer() {
        Career career = career(1L);
        when(careerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(career));
        when(studyPlanRepository.findByCareerIdAndVersion(1L, "2022.3"))
                .thenReturn(Optional.of(plan(2L, "2022.3", true, career)));

        assertThatThrownBy(() -> studyPlanService.create(1L, new StudyPlanRequest("2022.3", false)))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void createRejectsMissingCareer() {
        when(careerRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyPlanService.create(404L, new StudyPlanRequest("2023.1", false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setCurrentDeactivatesPreviousPlanForSameCareer() {
        Career career = career(1L);
        StudyPlan previous = plan(2L, "2022.3", true, career);
        StudyPlan selected = plan(3L, "2023.1", false, career);
        when(studyPlanRepository.findById(3L)).thenReturn(Optional.of(selected));
        when(careerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(career));
        when(studyPlanRepository.findAllByCareerIdAndCurrentPlanTrue(1L)).thenReturn(List.of(previous));
        when(studyPlanRepository.save(selected)).thenReturn(selected);

        var result = studyPlanService.setCurrent(3L);

        assertThat(result.currentPlan()).isTrue();
        assertThat(previous.isCurrentPlan()).isFalse();
    }

    @Test
    void setCurrentRejectsMissingPlan() {
        when(studyPlanRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyPlanService.setCurrent(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Career career(Long id) {
        Career career = new Career("10450", "Programming", "Description", 2000, 50);
        career.setId(id);
        return career;
    }

    private StudyPlan plan(Long id, String version, boolean current, Career career) {
        StudyPlan plan = new StudyPlan(version, current, career);
        plan.setId(id);
        return plan;
    }
}
