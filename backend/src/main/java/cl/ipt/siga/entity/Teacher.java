package cl.ipt.siga.entity;

import cl.ipt.siga.entity.enums.TeacherStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teachers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_teacher_national_id", columnNames = "national_id"),
        @UniqueConstraint(name = "uk_teacher_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_teacher_keycloak_subject", columnNames = "keycloak_subject")
})
@Getter
@Setter
@NoArgsConstructor
public class Teacher {

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

    @NotBlank
    @Column(nullable = false, length = 150)
    private String professionalTitle;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String academicDegree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeacherStatus status = TeacherStatus.ACTIVE;
}
