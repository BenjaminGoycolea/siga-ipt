package cl.ipt.siga.dto;

import cl.ipt.siga.entity.CourseEnrollment;
import cl.ipt.siga.entity.enums.CourseEnrollmentStatus;
import cl.ipt.siga.entity.enums.CourseResult;

import java.math.BigDecimal;
import java.time.Instant;

public record CourseEnrollmentResponse(
        Long id,
        Long studentId,
        String studentNationalId,
        String studentName,
        Long academicPeriodId,
        String academicPeriodCode,
        Long courseId,
        String courseCode,
        String courseName,
        Long sectionId,
        CourseEnrollmentStatus status,
        Instant registeredAt,
        BigDecimal finalGrade,
        CourseResult result
) {

    public static CourseEnrollmentResponse from(CourseEnrollment enrollment) {
        return new CourseEnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getNationalId(),
                enrollment.getStudent().getFullName(),
                enrollment.getAcademicPeriod().getId(),
                enrollment.getAcademicPeriod().getCode(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getCode(),
                enrollment.getCourse().getName(),
                enrollment.getSection().getId(),
                enrollment.getStatus(),
                enrollment.getRegisteredAt(),
                enrollment.getFinalGrade(),
                enrollment.getFinalGrade() == null
                        ? null
                        : enrollment.getFinalGrade().compareTo(BigDecimal.valueOf(4)) >= 0
                                ? CourseResult.PASSED
                                : CourseResult.FAILED
        );
    }
}
