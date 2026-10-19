package cl.ipt.siga.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_matriculations", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_student_matriculation_period",
                columnNames = {"student_id", "academic_period_id"}
        )
})
@Getter
@Setter
@NoArgsConstructor
public class StudentMatriculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_period_id", nullable = false)
    private AcademicPeriod academicPeriod;

    @Column(nullable = false)
    private boolean matriculated;

    public StudentMatriculation(Student student, AcademicPeriod academicPeriod, boolean matriculated) {
        this.student = student;
        this.academicPeriod = academicPeriod;
        this.matriculated = matriculated;
    }
}
