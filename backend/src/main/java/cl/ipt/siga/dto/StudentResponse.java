package cl.ipt.siga.dto;

import cl.ipt.siga.entity.Student;
import cl.ipt.siga.entity.enums.StudentStatus;

public record StudentResponse(
        Long id,
        String nationalId,
        String fullName,
        String email,
        StudentStatus status,
        Long careerId,
        String careerName,
        Long studyPlanId,
        String studyPlanVersion,
        boolean identityLinked
) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getNationalId(),
                student.getFullName(),
                student.getEmail(),
                student.getStatus(),
                student.getCareer().getId(),
                student.getCareer().getName(),
                student.getStudyPlan().getId(),
                student.getStudyPlan().getVersion(),
                student.getKeycloakSubject() != null
        );
    }
}
