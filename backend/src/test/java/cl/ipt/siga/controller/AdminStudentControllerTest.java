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
class AdminStudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void studentManagementRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentRoleCannotManageStudents() throws Exception {
        mockMvc.perform(get("/api/admin/students").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanRegisterStudentInCurrentCareerPlan() throws Exception {
        mockMvc.perform(post("/api/admin/students")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "11.111.111-1",
                                  "fullName": "Test Student",
                                  "email": "test.student@example.edu",
                                  "careerId": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REGULAR"))
                .andExpect(jsonPath("$.studyPlanVersion").value("2022.3"));
    }

    @Test
    void administratorCanRegisterStudentInCybersecurityPlan() throws Exception {
        mockMvc.perform(post("/api/admin/students")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "22.222.222-2",
                                  "fullName": "Test Student",
                                  "email": "another.student@example.edu",
                                  "careerId": 3
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studyPlanVersion").value("2021.3"));
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/students")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "33.333.333-3",
                                  "fullName": "Test Student",
                                  "email": "not-an-email",
                                  "careerId": 2
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
