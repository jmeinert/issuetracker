package de.jmeinert.issuetracker.project;

import de.jmeinert.issuetracker.BaseSecurityWebMvcTest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@WithMockUser(roles = "ADMIN")
class ProjectControllerTest extends BaseSecurityWebMvcTest {

    @MockitoBean
    private ProjectService projectService;

    @Test
    void getAll_returns200() throws Exception {
        when(projectService.findAll())
            .thenReturn(List.of(
                new Project("TestName", "TestDescription"),
                new Project("TestName2", "TestDescription2")
            ));

        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]['name']").value("TestName"))
            .andExpect(jsonPath("$[0]['description']").value("TestDescription"))
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithAnonymousUser
    void getAll_returns401_whenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Authentication required"))
            .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE));

        verifyNoInteractions(projectService);
    }

    @Test
    void getProjectById_returns200_whenProjectExists() throws Exception {
        when(projectService.findById(1L))
            .thenReturn(new Project("TestName", "TestDescription"));

        mockMvc.perform(get("/api/projects/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("TestName"))
            .andExpect(jsonPath("$.description").value("TestDescription"));
    }

    @Test
    void getProjectById_returns404_whenProjectDoesNotExist() throws Exception {
        when(projectService.findById(5L))
            .thenThrow(new ProjectNotFoundException(5L));

        mockMvc.perform(get("/api/projects/5"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Project not found with id: 5"));
    }

    @Test
    void createProject_returns201_whenRequestIsValid() throws Exception {
        String name = "TestName";
        String description = "TestDescription";

        ProjectRequest request = new ProjectRequest(name, description);

        when(projectService.create(request))
            .thenReturn(new Project(name, description));

        mockMvc.perform(post("/api/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "%s",
                    "description": "%s"
                }
                """.formatted(name, description)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.description").value(description));
    }

    @Test
    void createProject_returns400_whenNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "",
                    "description": "TestDescription"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.name").value("must not be blank"));

        verifyNoInteractions(projectService);
    }

    @Test
    void createProject_returns400_whenNameIsTooLong() throws Exception {
        String tooLongName = "a".repeat(151);

        mockMvc.perform(post("/api/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "%s",
                    "description": "TestDescription"
                }
                """.formatted(tooLongName)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.name").value("size must be between 0 and 150"));

        verifyNoInteractions(projectService);
    }

    @Test
    void createProject_returns400_whenDescriptionIsTooLong() throws Exception {
        String tooLongDescription = "a".repeat(1001);

        mockMvc.perform(post("/api/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "TestName",
                    "description": "%s"
                }
                """.formatted(tooLongDescription)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.description").value("size must be between 0 and 1000"));

        verifyNoInteractions(projectService);
    }

    @Test
    void updateProject_returns200_whenProjectExists() throws Exception {
        String name = "TestName";
        String description = "UpdatedTestDescription";

        ProjectRequest request = new ProjectRequest(name, description);

        when(projectService.update(1L, request))
            .thenReturn(new Project(name, description));

        mockMvc.perform(put("/api/projects/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "%s",
                    "description": "%s"
                }
                """.formatted(name, description)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.description").value(description));
    }

    @Test
    void updateProject_returns404_whenProjectDoesNotExist() throws Exception {
        String name = "TestName";
        String description = "UpdatedTestDescription";

        ProjectRequest request = new ProjectRequest(name, description);

        when(projectService.update(5L, request))
            .thenThrow(new ProjectNotFoundException(5L));

        mockMvc.perform(put("/api/projects/5")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "%s",
                    "description": "%s"
                }
                """.formatted(name, description)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Project not found with id: 5"));
    }

    @Test
    void updateProject_returns400_whenNameIsBlank() throws Exception {
        mockMvc.perform(put("/api/projects/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "",
                    "description": "UpdatedTestDescription"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.name").value("must not be blank"));

        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_returns204_whenProjectExists() throws Exception {
        mockMvc.perform(delete("/api/projects/1"))
            .andExpect(status().isNoContent());

        verify(projectService).delete(1L);
    }

    @Test
    void deleteProject_returns404_whenProjectDoesNotExist() throws Exception {
        doThrow(new ProjectNotFoundException(5L))
            .when(projectService)
            .delete(5L);

        mockMvc.perform(delete("/api/projects/5"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Project not found with id: 5"));
    }

    @Test
    void deleteProject_returns409_whenProjectHasIssues() throws Exception {
        doThrow(new ProjectHasIssuesException(1L))
            .when(projectService)
            .delete(1L);

        mockMvc.perform(delete("/api/projects/1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                .value("Project with id 1 cannot be deleted because it still contains issues."));
    }
}
