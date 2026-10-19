package cl.ipt.siga.dto;

import cl.ipt.siga.entity.StudyPlan;

public record StudyPlanResponse(
        Long id,
        Long careerId,
        String careerCode,
        String careerName,
        String version,
        boolean currentPlan
) {

    public static StudyPlanResponse from(StudyPlan studyPlan) {
        return new StudyPlanResponse(
                studyPlan.getId(),
                studyPlan.getCareer().getId(),
                studyPlan.getCareer().getCode(),
                studyPlan.getCareer().getName(),
                studyPlan.getVersion(),
                studyPlan.isCurrentPlan()
        );
    }
}
