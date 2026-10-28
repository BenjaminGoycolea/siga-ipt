package cl.ipt.siga.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminCareerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void careerManagementRequiresAdministratorRole() throws Exception {
        mockMvc.perform(get("/api/admin/careers"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/careers").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanListSeededCareers() throws Exception {
        mockMvc.perform(get("/api/admin/careers").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].durationSemesters").value(4));
    }

    @Test
    void administratorCanCreateCareer() throws Exception {
        mockMvc.perform(post("/api/admin/careers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "10453",
                                  "name": "Técnico de Prueba",
                                  "description": "Career created in an API test.",
                                  "startYear": 2026,
                                  "annualVacancies": 50
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("10453"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
