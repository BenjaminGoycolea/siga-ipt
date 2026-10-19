package cl.ipt.siga.dto;

import cl.ipt.siga.entity.Career;
import cl.ipt.siga.entity.enums.CareerStatus;

public record CareerResponse(
        Long id,
        String code,
        String name,
        String description,
        int startYear,
        int annualVacancies,
        int durationSemesters,
        CareerStatus status
) {

    public static CareerResponse from(Career career) {
        return new CareerResponse(
                career.getId(),
                career.getCode(),
                career.getName(),
                career.getDescription(),
                career.getStartYear(),
                career.getAnnualVacancies(),
                career.getDurationSemesters(),
                career.getStatus()
        );
    }
}
