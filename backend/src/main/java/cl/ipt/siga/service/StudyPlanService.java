package cl.ipt.siga.service;

import cl.ipt.siga.dto.StudyPlanRequest;
import cl.ipt.siga.dto.StudyPlanResponse;
import cl.ipt.siga.entity.Career;
import cl.ipt.siga.entity.StudyPlan;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CareerRepository;
import cl.ipt.siga.repository.StudyPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StudyPlanService {

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;

    public StudyPlanService(CareerRepository careerRepository, StudyPlanRepository studyPlanRepository) {
        this.careerRepository = careerRepository;
        this.studyPlanRepository = studyPlanRepository;
    }

    @Transactional(readOnly = true)
    public List<StudyPlanResponse> findByCareer(Long careerId) {
        getCareer(careerId);
        return studyPlanRepository.findByCareerIdOrderByVersionDesc(careerId).stream()
                .map(StudyPlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyPlanResponse findById(Long id) {
        return StudyPlanResponse.from(getStudyPlan(id));
    }

    public StudyPlanResponse create(Long careerId, StudyPlanRequest request) {
        Career career = lockCareer(careerId);
        String version = request.version().trim();
        if (studyPlanRepository.findByCareerIdAndVersion(careerId, version).isPresent()) {
            throw new BusinessConflictException("A study plan with this version already exists for the career.");
        }

        StudyPlan studyPlan = new StudyPlan(version, false, career);
        if (request.currentPlan()) {
            setAsCurrent(studyPlan);
        }
        return StudyPlanResponse.from(studyPlanRepository.save(studyPlan));
    }

    public StudyPlanResponse setCurrent(Long id) {
        StudyPlan studyPlan = getStudyPlan(id);
        Career career = lockCareer(studyPlan.getCareer().getId());
        studyPlan.setCareer(career);
        setAsCurrent(studyPlan);
        return StudyPlanResponse.from(studyPlanRepository.save(studyPlan));
    }

    private void setAsCurrent(StudyPlan studyPlan) {
        studyPlanRepository.findAllByCareerIdAndCurrentPlanTrue(studyPlan.getCareer().getId()).stream()
                .filter(current -> !current.getId().equals(studyPlan.getId()))
                .forEach(current -> current.setCurrentPlan(false));
        studyPlan.setCurrentPlan(true);
    }

    private Career getCareer(Long id) {
        return careerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found: " + id));
    }

    private Career lockCareer(Long id) {
        return careerRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found: " + id));
    }

    private StudyPlan getStudyPlan(Long id) {
        return studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found: " + id));
    }
}
