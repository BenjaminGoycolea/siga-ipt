package cl.ipt.siga.dto;

import cl.ipt.siga.entity.AcademicPeriod;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;

public record AcademicPeriodResponse(
        Long id,
        int year,
        int semester,
        String code,
        AcademicPeriodStatus status
) {

    public static AcademicPeriodResponse from(AcademicPeriod period) {
        return new AcademicPeriodResponse(
                period.getId(),
                period.getYear(),
                period.getSemester(),
                period.getCode(),
                period.getStatus()
        );
    }
}
