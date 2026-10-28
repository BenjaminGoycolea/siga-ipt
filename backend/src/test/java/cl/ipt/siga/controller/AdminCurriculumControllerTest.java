package cl.ipt.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cl.ipt.siga.repository.CareerRepository;
import cl.ipt.siga.repository.StudyPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminCurriculumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CareerRepository careerRepository;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void curriculumManagementRequiresAdministratorRole() throws Exception {
        Long careerId = careerRepository.findByCode("10450").orElseThrow().getId();
        mockMvc.perform(get("/api/admin/careers/{careerId}/study-plans", careerId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/careers/{careerId}/study-plans", careerId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanListSeededPlansAndCoursesWithPrerequisites() throws Exception {
        var career = careerRepository.findByCode("10452").orElseThrow();
        var plan = studyPlanRepository.findByCareerIdAndVersion(career.getId(), "2021.3").orElseThrow();

        mockMvc.perform(get("/api/admin/careers/{careerId}/study-plans", career.getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value("2021.3"))
                .andExpect(jsonPath("$[0].currentPlan").value(true));

        mockMvc.perform(get("/api/admin/study-plans/{planId}/courses", plan.getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(24)))
                .andExpect(jsonPath("$[12].code").value("TCS301"))
                .andExpect(jsonPath("$[12].prerequisiteCodes[1]").value("TCS204"));
    }

    @Test
    void invalidCourseRequestReturnsBadRequest() throws Exception {
        Long planId = studyPlanRepository.findAll().get(0).getId();
        mockMvc.perform(post("/api/admin/study-plans/{planId}/courses", planId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "",
                                  "name": "",
                                  "semester": 5,
                                  "theoryHours": -1,
                                  "exerciseHours": 0,
                                  "laboratoryHours": 0,
                                  "credits": 8,
                                  "prerequisiteIds": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administratorCanCreateCurrentPlanAndAddCourse() throws Exception {
        var career = careerRepository.findByCode("10450").orElseThrow();
        var previousPlan = studyPlanRepository.findByCareerIdAndCurrentPlanTrue(career.getId()).orElseThrow();
        String version = "test-" + UUID.randomUUID().toString().substring(0, 8);

        MvcResult planResult = mockMvc.perform(post("/api/admin/careers/{careerId}/study-plans", career.getId())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": "%s",
                                  "currentPlan": true
                                }
                                """.formatted(version)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentPlan").value(true))
                .andReturn();

        Long planId = objectMapper.readTree(planResult.getResponse().getContentAsString()).get("id").asLong();
        assertThat(
                studyPlanRepository.findById(previousPlan.getId()).orElseThrow().isCurrentPlan()
        ).isFalse();

        String courseCode = "TST" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        mockMvc.perform(post("/api/admin/study-plans/{planId}/courses", planId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "Test course",
                                  "semester": 1,
                                  "theoryHours": 2,
                                  "exerciseHours": 2,
                                  "laboratoryHours": 2,
                                  "credits": 4,
                                  "prerequisiteIds": []
                                }
                                """.formatted(courseCode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(courseCode))
                .andExpect(jsonPath("$.prerequisiteCodes", hasSize(0)));
    }
}
