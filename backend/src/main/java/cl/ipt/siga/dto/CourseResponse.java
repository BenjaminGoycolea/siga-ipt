package cl.ipt.siga.dto;

import cl.ipt.siga.entity.Course;

import java.util.List;

public record CourseResponse(
        Long id,
        Long studyPlanId,
        String code,
        String name,
        int semester,
        int theoryHours,
        int exerciseHours,
        int laboratoryHours,
        int credits,
        List<String> prerequisiteCodes
) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getStudyPlan().getId(),
                course.getCode(),
                course.getName(),
                course.getSemester(),
                course.getTheoryHours(),
                course.getExerciseHours(),
                course.getLaboratoryHours(),
                course.getCredits(),
                course.getPrerequisites().stream()
                        .map(prerequisite -> prerequisite.getCode())
                        .sorted()
                        .toList()
        );
    }
}
