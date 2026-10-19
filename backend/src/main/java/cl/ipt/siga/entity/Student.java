package cl.ipt.siga.entity;

import cl.ipt.siga.entity.enums.StudentStatus;
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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "students", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_national_id", columnNames = "national_id"),
        @UniqueConstraint(name = "uk_student_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_student_keycloak_subject", columnNames = "keycloak_subject")
})
@Getter
@Setter
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "national_id", nullable = false, length = 20)
    private String nationalId;

    @NotBlank
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Email
    @NotBlank
    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "keycloak_subject", length = 64)
    private String keycloakSubject;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StudentStatus status = StudentStatus.REGULAR;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "career_id", nullable = false)
    private Career career;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlan studyPlan;

    public Student(String nationalId, String fullName, String email, Career career, StudyPlan studyPlan) {
        this.nationalId = nationalId;
        this.fullName = fullName;
        this.email = email;
        this.career = career;
        this.studyPlan = studyPlan;
        this.status = StudentStatus.REGULAR;
    }
}
