package cl.ipt.siga.repository;

import cl.ipt.siga.entity.AcademicPeriod;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long> {

    List<AcademicPeriod> findAllByOrderByYearDescSemesterDesc();

    Optional<AcademicPeriod> findByYearAndSemester(int year, int semester);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select period from AcademicPeriod period where period.id = :id")
    Optional<AcademicPeriod> findByIdForUpdate(@Param("id") Long id);
}
