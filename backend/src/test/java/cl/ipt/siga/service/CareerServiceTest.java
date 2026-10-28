package cl.ipt.siga.service;

import cl.ipt.siga.dto.CareerRequest;
import cl.ipt.siga.entity.Career;
import cl.ipt.siga.entity.enums.CareerStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CareerRepository;
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
class CareerServiceTest {

    @Mock
    private CareerRepository careerRepository;

    @InjectMocks
    private CareerService careerService;

    @Test
    void findAllReturnsCareers() {
        when(careerRepository.findAll()).thenReturn(List.of(career(1L, "10450")));

        assertThat(careerService.findAll()).hasSize(1);
    }

    @Test
    void findByIdReturnsCareer() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, "10450")));

        assertThat(careerService.findById(1L).code()).isEqualTo("10450");
    }

    @Test
    void findByIdRejectsMissingCareer() {
        when(careerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> careerService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createNormalizesFieldsAndDefaultsActiveStatus() {
        when(careerRepository.findByCode("TST")).thenReturn(Optional.empty());
        when(careerRepository.save(any(Career.class))).thenAnswer(invocation -> {
            Career saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        var result = careerService.create(request(" tst ", " New career ", " Description "));

        assertThat(result.code()).isEqualTo("TST");
        assertThat(result.name()).isEqualTo("New career");
        assertThat(result.durationSemesters()).isEqualTo(4);
        assertThat(result.status()).isEqualTo(CareerStatus.ACTIVE);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(careerRepository.findByCode("10450")).thenReturn(Optional.of(career(1L, "10450")));

        assertThatThrownBy(() -> careerService.create(request("10450", "Name", "Description")))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void updateAllowsExistingCodeAndUpdatesCareer() {
        Career existing = career(1L, "10450");
        when(careerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(careerRepository.findByCode("10450")).thenReturn(Optional.of(existing));
        when(careerRepository.save(existing)).thenReturn(existing);

        var result = careerService.update(1L, request("10450", "Updated", "Updated description"));

        assertThat(result.name()).isEqualTo("Updated");
        assertThat(result.description()).isEqualTo("Updated description");
    }

    @Test
    void updateRejectsCodeUsedByAnotherCareer() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, "10450")));
        when(careerRepository.findByCode("10451")).thenReturn(Optional.of(career(2L, "10451")));

        assertThatThrownBy(() -> careerService.update(1L, request("10451", "Updated", "Description")))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void updateRejectsMissingCareer() {
        when(careerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> careerService.update(404L, request("TST", "Name", "Description")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatusPreservesCareerAndSavesStatus() {
        Career existing = career(1L, "10450");
        when(careerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(careerRepository.save(existing)).thenReturn(existing);

        var result = careerService.updateStatus(1L, CareerStatus.INACTIVE);

        assertThat(result.status()).isEqualTo(CareerStatus.INACTIVE);
        verify(careerRepository).save(existing);
    }

    private CareerRequest request(String code, String name, String description) {
        return new CareerRequest(code, name, description, 2020, 40);
    }

    private Career career(Long id, String code) {
        Career career = new Career(code, "Career " + code, "Career description", 2000, 50);
        career.setId(id);
        return career;
    }
}
