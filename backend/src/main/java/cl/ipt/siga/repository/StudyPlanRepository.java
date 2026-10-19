package cl.ipt.siga.repository;

import cl.ipt.siga.entity.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    List<StudyPlan> findByCareerIdOrderByVersionDesc(Long careerId);

    Optional<StudyPlan> findByCareerIdAndCurrentPlanTrue(Long careerId);

    List<StudyPlan> findAllByCareerIdAndCurrentPlanTrue(Long careerId);

    Optional<StudyPlan> findByCareerIdAndVersion(Long careerId, String version);
}
