package cl.ipt.siga.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "courses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_plan_code", columnNames = {"study_plan_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String code;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String name;

    @Min(1)
    @Max(4)
    @Column(nullable = false)
    private int semester;

    @Min(0)
    @Column(nullable = false)
    private int theoryHours;

    @Min(0)
    @Column(nullable = false)
    private int exerciseHours;

    @Min(0)
    @Column(nullable = false)
    private int laboratoryHours;

    @Min(1)
    @Max(7)
    @Column(nullable = false)
    private int credits;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlan studyPlan;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_prerequisites",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequisite_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_course_prerequisite",
                    columnNames = {"course_id", "prerequisite_id"}
            )
    )
    private Set<Course> prerequisites = new LinkedHashSet<>();

    public Course(
            String code,
            String name,
            int semester,
            int theoryHours,
            int exerciseHours,
            int laboratoryHours,
            int credits,
            StudyPlan studyPlan
    ) {
        this.code = code;
        this.name = name;
        this.semester = semester;
        this.theoryHours = theoryHours;
        this.exerciseHours = exerciseHours;
        this.laboratoryHours = laboratoryHours;
        this.credits = credits;
        this.studyPlan = studyPlan;
    }
}
