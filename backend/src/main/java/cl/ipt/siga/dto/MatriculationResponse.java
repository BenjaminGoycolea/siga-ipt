package cl.ipt.siga.dto;

import cl.ipt.siga.entity.StudentMatriculation;

public record MatriculationResponse(
        Long studentId,
        Long academicPeriodId,
        String academicPeriodCode,
        boolean matriculated
) {

    public static MatriculationResponse from(StudentMatriculation matriculation) {
        return new MatriculationResponse(
                matriculation.getStudent().getId(),
                matriculation.getAcademicPeriod().getId(),
                matriculation.getAcademicPeriod().getCode(),
                matriculation.isMatriculated()
        );
    }
}
