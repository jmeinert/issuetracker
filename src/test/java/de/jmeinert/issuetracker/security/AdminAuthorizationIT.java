package de.jmeinert.issuetracker.security;

import de.jmeinert.issuetracker.config.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@WithMockUser(roles = "ADMIN")
class AdminAuthorizationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createProject_grantsAccess_whenPrincipalIsAnAdmin() throws Exception {
        mockMvc.perform(post("/api/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "TestProject",
                    "description": "TestDescription"
                }
                """))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProject_returns403_whenPrincipalIsNotAnAdmin() throws Exception {
        mockMvc.perform(post("/api/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "TestProject",
                    "description": "TestDescription"
                }
                """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void updateProject_grantsAccess_whenPrincipalIsAnAdmin() throws Exception {
        mockMvc.perform(put("/api/projects/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "TestProject",
                    "description": "TestDescription"
                }
                """))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateProject_returns403_whenPrincipalIsNotAnAdmin() throws Exception {
        mockMvc.perform(put("/api/projects/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "TestProject",
                    "description": "TestDescription"
                }
                """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void deleteProject_grantsAccess_whenPrincipalIsAnAdmin() throws Exception {
        mockMvc.perform(delete("/api/projects/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteProject_returns403_whenPrincipalIsNotAnAdmin() throws Exception {
        mockMvc.perform(delete("/api/projects/1"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void assignIssue_grantsAccess_whenPrincipalIsAnAdmin() throws Exception {
        mockMvc.perform(patch("/api/issues/1/assignee")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "assigneeId": 2
                }
                """))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void assignIssue_returns403_whenPrincipalIsNotAnAdmin() throws Exception {
        mockMvc.perform(patch("/api/issues/1/assignee")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "assigneeId": 2
                }
                """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void unassignIssue_grantsAccess_whenPrincipalIsAnAdmin() throws Exception {
        mockMvc.perform(delete("/api/issues/1/assignee"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void unassignIssue_returns403_whenPrincipalIsNotAnAdmin() throws Exception {
        mockMvc.perform(delete("/api/issues/1/assignee"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void deleteIssue_grantsAccess_whenPrincipalIsAnAdmin() throws Exception {
        mockMvc.perform(delete("/api/issues/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteIssue_returns403_whenPrincipalIsNotAnAdmin() throws Exception {
        mockMvc.perform(delete("/api/issues/1"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void changeUserEnabled_grantsAccess_whenPrincipalIsAnAdmin() throws Exception {
        mockMvc.perform(patch("/api/users/1/enabled")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "enabled": false
                }
                """))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void changeUserEnabled_returns403_whenPrincipalIsNotAnAdmin() throws Exception {
        mockMvc.perform(patch("/api/users/1/enabled")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "enabled": false
                }
                """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }
}
