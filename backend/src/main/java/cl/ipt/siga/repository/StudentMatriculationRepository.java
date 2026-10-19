package cl.ipt.siga.repository;

import cl.ipt.siga.entity.StudentMatriculation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentMatriculationRepository extends JpaRepository<StudentMatriculation, Long> {

    Optional<StudentMatriculation> findByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);
}
