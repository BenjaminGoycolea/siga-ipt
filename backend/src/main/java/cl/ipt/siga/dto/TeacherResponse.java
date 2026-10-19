package cl.ipt.siga.dto;

import cl.ipt.siga.entity.Teacher;
import cl.ipt.siga.entity.enums.TeacherStatus;

public record TeacherResponse(
        Long id,
        String nationalId,
        String fullName,
        String email,
        String professionalTitle,
        String academicDegree,
        TeacherStatus status,
        boolean identityLinked
) {

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getNationalId(),
                teacher.getFullName(),
                teacher.getEmail(),
                teacher.getProfessionalTitle(),
                teacher.getAcademicDegree(),
                teacher.getStatus(),
                teacher.getKeycloakSubject() != null
        );
    }
}
