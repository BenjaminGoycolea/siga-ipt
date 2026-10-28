package cl.ipt.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cl.ipt.siga.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Locale;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminOfferingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void periodManagementRequiresAdministratorRole() throws Exception {
        mockMvc.perform(get("/api/admin/periods"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/periods").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanCreatePeriodAndSectionWithCrossEntityRules() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int year = 3000 + Math.floorMod(suffix.hashCode(), 6000);
        String nationalId = "RUN-" + suffix.toUpperCase(Locale.ROOT);
        String email = suffix + "@example.edu";

        MvcResult periodResult = mockMvc.perform(post("/api/admin/periods")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": %d,
                                  "semester": 1
                                }
                                """.formatted(year)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(year + "-1"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        long periodId = objectMapper.readTree(periodResult.getResponse().getContentAsString())
                .get("id").asLong();

        MvcResult teacherResult = mockMvc.perform(post("/api/admin/teachers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "%s",
                                  "fullName": "Offering Test Teacher",
                                  "email": "%s",
                                  "professionalTitle": "Engineer",
                                  "academicDegree": "Master"
                                }
                                """.formatted(nationalId, email)))
                .andExpect(status().isCreated())
                .andReturn();
        long teacherId = objectMapper.readTree(teacherResult.getResponse().getContentAsString())
                .get("id").asLong();
        long courseId = courseRepository.findAll().stream()
                .filter(course -> course.getCode().equals("TAP101"))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/admin/periods/{periodId}/sections", periodId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "scheduleBlocks": ["L1", "W2", "V3"]
                                }
                                """.formatted(courseId, teacherId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.academicPeriodCode").value(year + "-1"))
                .andExpect(jsonPath("$.courseCode").value("TAP101"))
                .andExpect(jsonPath("$.capacity").value(40));

        mockMvc.perform(put("/api/admin/teachers/{id}/status", teacherId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/admin/periods/{id}/status", periodId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CLOSED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(put("/api/admin/teachers/{id}/status", teacherId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void invalidPeriodSemesterIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/periods")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2027,"semester":3}
                                """))
                .andExpect(status().isBadRequest());
    }
}
