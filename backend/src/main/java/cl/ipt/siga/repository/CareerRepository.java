package cl.ipt.siga.repository;

import cl.ipt.siga.entity.Career;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CareerRepository extends JpaRepository<Career, Long> {

    Optional<Career> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select career from Career career where career.id = :id")
    Optional<Career> findByIdForUpdate(@Param("id") Long id);
}
