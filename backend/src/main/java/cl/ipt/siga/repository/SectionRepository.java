package cl.ipt.siga.repository;

import cl.ipt.siga.entity.Section;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByAcademicPeriodIdOrderByCourseCodeAsc(Long academicPeriodId);

    List<Section> findByTeacherIdAndAcademicPeriodId(Long teacherId, Long academicPeriodId);

    Optional<Section> findByAcademicPeriodIdAndCourseId(Long academicPeriodId, Long courseId);

    boolean existsByTeacherIdAndAcademicPeriod_Status(Long teacherId, AcademicPeriodStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select section from Section section where section.id = :id")
    Optional<Section> findByIdForUpdate(@Param("id") Long id);
}
