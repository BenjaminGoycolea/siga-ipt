package cl.ipt.siga.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cl.ipt.siga.repository.CareerRepository;
import cl.ipt.siga.repository.CourseRepository;
import cl.ipt.siga.repository.StudyPlanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentAcademicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CareerRepository careerRepository;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    private Long previousCurrentPlanId;

    @AfterEach
    void restorePreviousCurrentPlan() throws Exception {
        if (previousCurrentPlanId != null) {
            mockMvc.perform(put("/api/admin/study-plans/{id}/current", previousCurrentPlanId)
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void studentCanEnrollOnlyWhenMatriculatedAndAvailableSeatsExist() throws Exception {
        mockMvc.perform(get("/api/student/periods/1/offerings"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/student/periods/1/offerings").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int year = 3000 + Math.floorMod(suffix.hashCode(), 6000);
        long periodId = createPeriod(year);
        long careerId = careerRepository.findByCode("10450").orElseThrow().getId();
        previousCurrentPlanId = studyPlanRepository.findByCareerIdAndCurrentPlanTrue(careerId)
                .orElseThrow()
                .getId();
        long tapPlanId = studyPlanRepository.findByCareerIdAndVersion(careerId, "2021.3")
                .orElseThrow()
                .getId();
        mockMvc.perform(put("/api/admin/study-plans/{id}/current", tapPlanId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        long teacherId = createTeacher(suffix);
        String teacherSubject = UUID.randomUUID().toString();
        linkTeacher(teacherId, teacherSubject);
        long studentId = createStudent(suffix, careerId);
        long tapPlanIdForCourse = coursePlanId("TAP101");
        assertThat(tapPlanIdForCourse).isEqualTo(tapPlanId);
        assertThat(coursePlanId("TAP201")).isEqualTo(tapPlanId);
        mockMvc.perform(get("/api/admin/students/{id}", studentId).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyPlanId").value(tapPlanId));
        String studentSubject = UUID.randomUUID().toString();
        linkStudent(studentId, studentSubject);
        setMatriculation(studentId, periodId, true);

        long firstSection = createSection(periodId, teacherId, "TAP101", "[\"L1\", \"L2\", \"L3\"]");
        long secondSection = createSection(periodId, teacherId, "TAP102", "[\"M1\", \"M2\"]");
        long thirdSection = createSection(periodId, teacherId, "TAP103", "[\"W1\", \"W2\"]");
        long conflictingTeacherId = createTeacher(suffix + "C");
        long conflictingSection = createSection(periodId, conflictingTeacherId, "TAP104", "[\"M1\", \"M2\"]");
        long advancedTeacherId = createTeacher(suffix + "H");
        long advancedSection = createSection(periodId, advancedTeacherId, "TAP201", "[\"J1\", \"J2\", \"J3\"]");
        String selection = """
                {"sectionIds":[%d,%d,%d]}
                """.formatted(firstSection, secondSection, thirdSection);

        mockMvc.perform(put("/api/student/periods/{periodId}/enrollments", periodId)
                        .with(studentJwt(studentSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sectionIds":[%d,%d,%d]}
                                """.formatted(advancedSection, secondSection, thirdSection)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("All prerequisites must be passed before enrolling in a course."));

        mockMvc.perform(put("/api/student/periods/{periodId}/enrollments", periodId)
                        .with(studentJwt(studentSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sectionIds":[%d,%d,%d]}
                                """.formatted(firstSection, secondSection, conflictingSection)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Selected sections cannot have conflicting schedules."));

        mockMvc.perform(put("/api/student/periods/{periodId}/enrollments", periodId)
                        .with(studentJwt(studentSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sectionIds":[%d,%d]}
                                """.formatted(firstSection, secondSection)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Students must enroll in 3 to 6 courses for this period."));

        MvcResult enrollmentResult = mockMvc.perform(put("/api/student/periods/{periodId}/enrollments", periodId)
                        .with(studentJwt(studentSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selection))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].status").value("ENROLLED"))
                .andReturn();
        JsonNode enrollments = objectMapper.readTree(enrollmentResult.getResponse().getContentAsString());
        long enrollmentId = enrollments
                .get(0)
                .get("id")
                .asLong();
        long anotherEnrollmentId = enrollments.get(1).get("id").asLong();

        mockMvc.perform(put("/api/admin/sections/{id}", firstSection)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId":%d,
                                  "teacherId":%d,
                                  "capacity":1,
                                  "scheduleBlocks":["L1","L2"]
                                }
                                """.formatted(courseId("TAP105"), teacherId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("A section with enrolled students cannot change its course or schedule."));

        mockMvc.perform(put("/api/admin/students/{studentId}/periods/{periodId}/matriculation", studentId, periodId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matriculated":false}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/student/periods/{periodId}/enrollments/{enrollmentId}", periodId, enrollmentId)
                        .with(studentJwt(studentSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        long unmatriculatedStudentId = createStudent("U" + suffix, careerId);
        String unmatriculatedSubject = UUID.randomUUID().toString();
        linkStudent(unmatriculatedStudentId, unmatriculatedSubject);
        mockMvc.perform(put("/api/student/periods/{periodId}/enrollments", periodId)
                        .with(studentJwt(unmatriculatedSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selection))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The student is not matriculated in this academic period."));

        long secondStudentId = createStudent("M" + suffix, careerId);
        String secondStudentSubject = UUID.randomUUID().toString();
        linkStudent(secondStudentId, secondStudentSubject);
        setMatriculation(secondStudentId, periodId, true);
        mockMvc.perform(delete("/api/student/periods/{periodId}/enrollments/{enrollmentId}", periodId, enrollmentId)
                        .with(studentJwt(secondStudentSubject)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/student/periods/{periodId}/enrollments", periodId)
                        .with(studentJwt(secondStudentSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selection))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The selected section has no available seats."));

        mockMvc.perform(put("/api/admin/periods/{id}/status", periodId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("Final grades are missing")
                ));

        mockMvc.perform(get("/api/teacher/sections/{sectionId}/enrollments", secondSection)
                        .with(teacherJwt(teacherSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/teacher/sections/{sectionId}/enrollments", secondSection)
                        .with(studentJwt(studentSubject)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teacher/sections/{sectionId}/enrollments", conflictingSection)
                        .with(teacherJwt(teacherSubject)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/teacher/sections/{sectionId}/enrollments/{enrollmentId}/grade",
                        secondSection, anotherEnrollmentId)
                        .with(teacherJwt(teacherSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"finalGrade\":7.1}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/teacher/sections/{sectionId}/enrollments/{enrollmentId}/grade",
                        secondSection, anotherEnrollmentId)
                        .with(teacherJwt(teacherSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"finalGrade\":5.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("PASSED"));
        long thirdEnrollmentId = enrollments.get(2).get("id").asLong();
        mockMvc.perform(put("/api/teacher/sections/{sectionId}/enrollments/{enrollmentId}/grade",
                        thirdSection, thirdEnrollmentId)
                        .with(teacherJwt(teacherSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"finalGrade\":3.9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("FAILED"));
        mockMvc.perform(put("/api/admin/periods/{id}/status", periodId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/student/academic-history")
                        .with(studentJwt(studentSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/admin/students/{id}/academic-history", studentId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(put("/api/admin/periods/{id}/status", periodId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/student/periods/{periodId}/enrollments/{enrollmentId}", periodId,
                        anotherEnrollmentId)
                        .with(studentJwt(studentSubject)))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/teacher/sections/{sectionId}/enrollments/{enrollmentId}/grade",
                        secondSection, anotherEnrollmentId)
                        .with(teacherJwt(teacherSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"finalGrade\":6.0}"))
                .andExpect(status().isConflict());
    }

    private long createPeriod(int year) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/periods")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":%d,"semester":1}
                                """.formatted(year)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createTeacher(String suffix) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/teachers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId":"TEACHER-%s",
                                  "fullName":"Enrollment Test Teacher",
                                  "email":"teacher-%s@example.edu",
                                  "professionalTitle":"Engineer",
                                  "academicDegree":"Master"
                                }
                                """.formatted(suffix, suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createStudent(String suffix, long careerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/students")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId":"STUDENT-%s",
                                  "fullName":"Enrollment Test Student",
                                  "email":"student-%s@example.edu",
                                  "careerId":%d
                                }
                                """.formatted(suffix, suffix, careerId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void linkStudent(long studentId, String subject) throws Exception {
        mockMvc.perform(put("/api/admin/students/{id}/keycloak-subject", studentId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keycloakSubject":"%s"}
                                """.formatted(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityLinked").value(true));
    }

    private void linkTeacher(long teacherId, String subject) throws Exception {
        mockMvc.perform(put("/api/admin/teachers/{id}/keycloak-subject", teacherId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keycloakSubject":"%s"}
                                """.formatted(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityLinked").value(true));
    }

    private void setMatriculation(long studentId, long periodId, boolean matriculated) throws Exception {
        mockMvc.perform(put("/api/admin/students/{studentId}/periods/{periodId}/matriculation", studentId, periodId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matriculated":%s}
                                """.formatted(matriculated)))
                .andExpect(status().isOk());
    }

    private long createSection(long periodId, long teacherId, String code, String blocks) throws Exception {
        long courseId = courseId(code);
        MvcResult result = mockMvc.perform(post("/api/admin/periods/{periodId}/sections", periodId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId":%d,
                                  "teacherId":%d,
                                  "capacity":1,
                                  "scheduleBlocks":%s
                                }
                                """.formatted(courseId, teacherId, blocks)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long courseId(String code) {
        return courseRepository.findAll().stream()
                .filter(course -> course.getCode().equals(code))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private long coursePlanId(String code) {
        return courseRepository.findAll().stream()
                .filter(course -> course.getCode().equals(code))
                .findFirst()
                .orElseThrow()
                .getStudyPlan()
                .getId();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor studentJwt(String subject) {
        return jwt().jwt(token -> token.subject(subject)).authorities(createAuthorityList("ROLE_STUDENT"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor teacherJwt(String subject) {
        return jwt().jwt(token -> token.subject(subject)).authorities(createAuthorityList("ROLE_TEACHER"));
    }
}
