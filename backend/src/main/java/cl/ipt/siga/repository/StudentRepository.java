package cl.ipt.siga.repository;

import cl.ipt.siga.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByNationalId(String nationalId);

    Optional<Student> findByEmailIgnoreCase(String email);

    Optional<Student> findByKeycloakSubject(String keycloakSubject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select student from Student student where student.keycloakSubject = :subject")
    Optional<Student> findByKeycloakSubjectForUpdate(@Param("subject") String subject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select student from Student student where student.id = :id")
    Optional<Student> findByIdForUpdate(@Param("id") Long id);

    List<Student> findByCareerIdOrderByFullNameAsc(Long careerId);
}
