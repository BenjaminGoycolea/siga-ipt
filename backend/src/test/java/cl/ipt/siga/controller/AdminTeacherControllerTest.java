package cl.ipt.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminTeacherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherManagementRequiresAdministratorRole() throws Exception {
        mockMvc.perform(get("/api/admin/teachers"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/teachers").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanRegisterAndUpdateTeacher() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String nationalId = "RUN-" + suffix.toUpperCase(java.util.Locale.ROOT);
        String email = suffix + "@example.edu";

        MvcResult createResult = mockMvc.perform(post("/api/admin/teachers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "%s",
                                  "fullName": "Test Teacher",
                                  "email": "%s",
                                  "professionalTitle": "Engineer",
                                  "academicDegree": "Master"
                                }
                                """.formatted(nationalId, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nationalId").value(nationalId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        long teacherId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();
        mockMvc.perform(put("/api/admin/teachers/{id}", teacherId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "%s",
                                  "fullName": "Updated Teacher",
                                  "email": "%s",
                                  "professionalTitle": "Senior Engineer",
                                  "academicDegree": "Doctorate"
                                }
                                """.formatted(nationalId, email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Teacher"))
                .andExpect(jsonPath("$.professionalTitle").value("Senior Engineer"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void invalidTeacherRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/teachers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "",
                                  "fullName": "",
                                  "email": "not-an-email",
                                  "professionalTitle": "",
                                  "academicDegree": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administratorCanChangeTeacherStatus() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String nationalId = "RUN-" + suffix;
        String email = suffix + "@example.edu";
        var createResult = mockMvc.perform(post("/api/admin/teachers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "%s",
                                  "fullName": "Status Teacher",
                                  "email": "%s",
                                  "professionalTitle": "Engineer",
                                  "academicDegree": "Master"
                                }
                                """.formatted(nationalId, email)))
                .andExpect(status().isCreated())
                .andReturn();

        var json = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long teacherId = json.get("id").asLong();

        mockMvc.perform(put("/api/admin/teachers/{id}/status", teacherId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }
}
