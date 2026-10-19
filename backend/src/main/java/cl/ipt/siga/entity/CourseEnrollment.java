package cl.ipt.siga.entity;

import cl.ipt.siga.entity.enums.CourseEnrollmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "course_enrollments", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_enrollment_student_course_period",
                columnNames = {"student_id", "course_id", "academic_period_id"}
        )
})
@Getter
@Setter
@NoArgsConstructor
public class CourseEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_period_id", nullable = false)
    private AcademicPeriod academicPeriod;

    @NotNull
    @Column(nullable = false)
    private Instant registeredAt = Instant.now();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseEnrollmentStatus status = CourseEnrollmentStatus.ENROLLED;

    @DecimalMin("1.0")
    @DecimalMax("7.0")
    @Column(precision = 2, scale = 1)
    private BigDecimal finalGrade;

    public CourseEnrollment(Student student, Course course, Section section, AcademicPeriod academicPeriod) {
        this.student = student;
        this.course = course;
        this.section = section;
        this.academicPeriod = academicPeriod;
    }
}
