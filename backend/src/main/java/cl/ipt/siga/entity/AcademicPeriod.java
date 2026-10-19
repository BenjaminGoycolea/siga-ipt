package cl.ipt.siga.entity;

import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "academic_periods", uniqueConstraints = {
        @UniqueConstraint(name = "uk_academic_period_year_semester", columnNames = {"academic_year", "semester"})
})
@Getter
@Setter
@NoArgsConstructor
public class AcademicPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(2000)
    @Max(9999)
    @Column(name = "academic_year", nullable = false)
    private int year;

    @Min(1)
    @Max(2)
    @Column(nullable = false)
    private int semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcademicPeriodStatus status = AcademicPeriodStatus.OPEN;

    public AcademicPeriod(int year, int semester) {
        this.year = year;
        this.semester = semester;
        this.status = AcademicPeriodStatus.OPEN;
    }

    public String getCode() {
        return year + "-" + semester;
    }
}
