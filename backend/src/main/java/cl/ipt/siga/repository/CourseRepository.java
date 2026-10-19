package cl.ipt.siga.repository;

import cl.ipt.siga.entity.Course;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStudyPlanIdOrderBySemesterAscCodeAsc(Long studyPlanId);

    Optional<Course> findByStudyPlanIdAndCode(Long studyPlanId, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select course from Course course where course.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);

    boolean existsByPrerequisitesId(Long prerequisiteId);

    List<Course> findByPrerequisitesId(Long prerequisiteId);
}
