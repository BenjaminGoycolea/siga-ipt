package cl.ipt.siga.entity;

import cl.ipt.siga.entity.enums.CareerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "careers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_career_code", columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
public class Career {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String code;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String description;

    @Min(1)
    @Column(nullable = false)
    private int startYear;

    @Min(1)
    @Column(nullable = false)
    private int annualVacancies;

    @Min(1)
    @Column(nullable = false)
    private int durationSemesters = 4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareerStatus status = CareerStatus.ACTIVE;

    public Career(String code, String name, String description, int startYear, int annualVacancies) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.startYear = startYear;
        this.annualVacancies = annualVacancies;
        this.durationSemesters = 4;
        this.status = CareerStatus.ACTIVE;
    }
}
