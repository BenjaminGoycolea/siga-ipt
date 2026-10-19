package cl.ipt.siga.dto;

import cl.ipt.siga.entity.Section;

import java.util.List;

public record SectionResponse(
        Long id,
        Long academicPeriodId,
        String academicPeriodCode,
        Long courseId,
        String courseCode,
        String courseName,
        Long teacherId,
        String teacherName,
        int capacity,
        List<String> scheduleBlocks
) {

    public static SectionResponse from(Section section) {
        return new SectionResponse(
                section.getId(),
                section.getAcademicPeriod().getId(),
                section.getAcademicPeriod().getCode(),
                section.getCourse().getId(),
                section.getCourse().getCode(),
                section.getCourse().getName(),
                section.getTeacher().getId(),
                section.getTeacher().getFullName(),
                section.getCapacity(),
                section.getScheduleBlocks().stream().sorted().toList()
        );
    }
}
