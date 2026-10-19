package cl.ipt.siga.repository;

import cl.ipt.siga.entity.Teacher;
import cl.ipt.siga.entity.enums.TeacherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByNationalId(String nationalId);

    Optional<Teacher> findByEmailIgnoreCase(String email);

    Optional<Teacher> findByKeycloakSubject(String keycloakSubject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select teacher from Teacher teacher where teacher.id = :id")
    Optional<Teacher> findByIdForUpdate(@Param("id") Long id);

    List<Teacher> findAllByOrderByFullNameAsc();

    List<Teacher> findByStatusOrderByFullNameAsc(TeacherStatus status);
}
