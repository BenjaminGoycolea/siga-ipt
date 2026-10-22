package cl.ipt.siga.service;

import cl.ipt.siga.dto.CareerRequest;
import cl.ipt.siga.dto.CareerResponse;
import cl.ipt.siga.entity.Career;
import cl.ipt.siga.entity.enums.CareerStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CareerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class CareerService {

    private final CareerRepository careerRepository;

    public CareerService(CareerRepository careerRepository) {
        this.careerRepository = careerRepository;
    }

    @Transactional(readOnly = true)
    public List<CareerResponse> findAll() {
        return careerRepository.findAll().stream()
                .map(CareerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CareerResponse findById(Long id) {
        return CareerResponse.from(getCareer(id));
    }

    public CareerResponse create(CareerRequest request) {
        String code = normalizeCode(request.code());
        ensureCodeAvailable(code, null);

        Career career = new Career(
                code,
                request.name().trim(),
                request.description().trim(),
                request.startYear(),
                request.annualVacancies()
        );
        return CareerResponse.from(careerRepository.save(career));
    }

    public CareerResponse update(Long id, CareerRequest request) {
        Career career = getCareer(id);
        String code = normalizeCode(request.code());
        ensureCodeAvailable(code, id);

        career.setCode(code);
        career.setName(request.name().trim());
        career.setDescription(request.description().trim());
        career.setStartYear(request.startYear());
        career.setAnnualVacancies(request.annualVacancies());
        return CareerResponse.from(careerRepository.save(career));
    }

    public CareerResponse updateStatus(Long id, CareerStatus status) {
        Career career = getCareer(id);
        career.setStatus(status);
        return CareerResponse.from(careerRepository.save(career));
    }

    private Career getCareer(Long id) {
        return careerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found: " + id));
    }

    private void ensureCodeAvailable(String code, Long excludedCareerId) {
        careerRepository.findByCode(code)
                .filter(existing -> !existing.getId().equals(excludedCareerId))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("A career with this code already exists.");
                });
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
