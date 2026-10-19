package cl.ipt.siga.repository;

import cl.ipt.siga.entity.CourseEnrollment;
import cl.ipt.siga.entity.enums.CourseEnrollmentStatus;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    List<CourseEnrollment> findByStudentIdAndAcademicPeriodIdOrderByRegisteredAtAsc(
            Long studentId,
            Long academicPeriodId
    );

    Optional<CourseEnrollment> findByStudentIdAndAcademicPeriodIdAndCourseId(
            Long studentId,
            Long academicPeriodId,
            Long courseId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select enrollment from CourseEnrollment enrollment where enrollment.id = :id")
    Optional<CourseEnrollment> findByIdForUpdate(@Param("id") Long id);

    int countBySectionIdAndStatus(Long sectionId, CourseEnrollmentStatus status);

    @Query("select case when count(enrollment) > 0 then true else false end "
            + "from CourseEnrollment enrollment "
            + "where enrollment.student.id = :studentId and enrollment.course.id = :courseId "
            + "and enrollment.finalGrade >= :minimumGrade "
            + "and enrollment.academicPeriod.status = cl.ipt.siga.entity.enums.AcademicPeriodStatus.CLOSED")
    boolean existsByStudentIdAndCourseIdAndFinalGradeGreaterThanEqual(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            @Param("minimumGrade") java.math.BigDecimal minimumGrade
    );

    boolean existsBySectionIdAndStatus(Long sectionId, CourseEnrollmentStatus status);

    @Query("select case when count(enrollment) > 0 then true else false end "
            + "from CourseEnrollment enrollment where enrollment.course.id = :courseId "
            + "and enrollment.finalGrade is not null "
            + "and enrollment.academicPeriod.status = cl.ipt.siga.entity.enums.AcademicPeriodStatus.CLOSED")
    boolean existsInAcademicHistory(@Param("courseId") Long courseId);

    boolean existsByStudentIdAndAcademicPeriodIdAndStatus(
            Long studentId,
            Long academicPeriodId,
            CourseEnrollmentStatus status
    );

    @Query("select enrollment from CourseEnrollment enrollment "
            + "join fetch enrollment.student "
            + "where enrollment.academicPeriod.id = :periodId and enrollment.status = :status "
            + "order by enrollment.student.id, enrollment.course.code")
    List<CourseEnrollment> findByAcademicPeriodIdAndStatusWithStudent(
            @Param("periodId") Long periodId,
            @Param("status") CourseEnrollmentStatus status
    );

    @Query("select enrollment from CourseEnrollment enrollment "
            + "join fetch enrollment.student "
            + "where enrollment.section.id = :sectionId and enrollment.status = :status "
            + "order by enrollment.student.fullName")
    List<CourseEnrollment> findBySectionIdAndStatusOrderByStudentName(
            @Param("sectionId") Long sectionId,
            @Param("status") CourseEnrollmentStatus status
    );

    @Query("select enrollment from CourseEnrollment enrollment "
            + "join fetch enrollment.course "
            + "join fetch enrollment.academicPeriod "
            + "where enrollment.student.id = :studentId and enrollment.status = :enrollmentStatus "
            + "and enrollment.finalGrade is not null and enrollment.academicPeriod.status = :periodStatus "
            + "order by enrollment.academicPeriod.year, enrollment.academicPeriod.semester, enrollment.course.code")
    List<CourseEnrollment> findAcademicHistory(
            @Param("studentId") Long studentId,
            @Param("enrollmentStatus") CourseEnrollmentStatus enrollmentStatus,
            @Param("periodStatus") AcademicPeriodStatus periodStatus
    );

    @Query("select count(enrollment) from CourseEnrollment enrollment "
            + "where enrollment.student.id = :studentId and enrollment.course.id = :courseId "
            + "and enrollment.finalGrade < :passingGrade "
            + "and enrollment.academicPeriod.status = cl.ipt.siga.entity.enums.AcademicPeriodStatus.CLOSED")
    long countFailedAttempts(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            @Param("passingGrade") java.math.BigDecimal passingGrade
    );

    @Query("select distinct enrollment.course.id from CourseEnrollment enrollment "
            + "where enrollment.student.id = :studentId and enrollment.finalGrade >= 4.0 "
            + "and enrollment.academicPeriod.status = cl.ipt.siga.entity.enums.AcademicPeriodStatus.CLOSED")
    Set<Long> findPassedCourseIdsByStudentId(@Param("studentId") Long studentId);
}
