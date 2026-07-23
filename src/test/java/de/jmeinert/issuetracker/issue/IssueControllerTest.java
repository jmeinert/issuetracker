package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IssueController.class)
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueService issueService;

    @Test
    void getIssueById_returns200_whenIssueExists() throws Exception {
        Long issueId = 1L;
        Long projectId = 2L;

        Project project = new Project("TestName", "TestDescription");
        ReflectionTestUtils.setField(project, "id", projectId);

        Issue issue = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );

        when(issueService.findById(issueId))
            .thenReturn(issue);

        mockMvc.perform(get("/api/issues/{issueId}", issueId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("TestTitle"))
            .andExpect(jsonPath("$.description").value("TestDescription"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.priority").value("LOW"))
            .andExpect(jsonPath("$.projectId").value(projectId));
    }

    @Test
    void getIssueById_returns404_whenIssueDoesNotExist() throws Exception {
        Long issueId = 5L;

        when(issueService.findById(issueId))
            .thenThrow(new IssueNotFoundException(issueId));

        mockMvc.perform(get("/api/issues/{issueId}", issueId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Issue not found with id: " + issueId));
    }

    @Test
    void getIssuesByProjectId_returns200_whenProjectExists() throws Exception {
        Long projectId = 1L;
        Project project = new Project("TestName", "TestDescription");
        ReflectionTestUtils.setField(project, "id", projectId);

        List<Issue> issues = List.of(
            new Issue(
                "TestTitle",
                "TestDescription",
                IssueStatus.OPEN,
                IssuePriority.LOW,
                project
            ),
            new Issue(
                "TestTitle2",
                "TestDescription2",
                IssueStatus.OPEN,
                IssuePriority.HIGH,
                project
            )
        );

        when(issueService.findAllByProjectId(projectId))
            .thenReturn(issues);

        mockMvc.perform(get("/api/projects/{projectId}/issues", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("TestTitle"))
            .andExpect(jsonPath("$[0].description").value("TestDescription"))
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getIssuesByProjectId_returns404_whenProjectDoesNotExist() throws Exception {
        Long projectId = 5L;

        when(issueService.findAllByProjectId(projectId))
            .thenThrow(new ProjectNotFoundException(projectId));

        mockMvc.perform(get("/api/projects/{projectId}/issues", projectId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Project not found with id: " + projectId));
    }

    @Test
    void createIssue_returns201_whenRequestIsValid() throws Exception {
        Long projectId = 1L;

        Project project = new Project("TestName", "TestDescription");
        ReflectionTestUtils.setField(project, "id", projectId);

        Issue issue = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );

        when(issueService.create(eq(projectId), any(CreateIssueRequest.class)))
            .thenReturn(issue);

        mockMvc.perform(post("/api/projects/{projectId}/issues", projectId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "TestDescription",
                    "status": "OPEN",
                    "priority": "LOW"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("TestTitle"))
            .andExpect(jsonPath("$.description").value("TestDescription"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.priority").value("LOW"))
            .andExpect(jsonPath("$.projectId").value(projectId));

        verify(issueService).create(eq(projectId), any(CreateIssueRequest.class));
    }

    @Test
    void createIssue_returns404_whenProjectDoesNotExist() throws Exception {
        Long projectId = 5L;

        when(issueService.create(eq(projectId), any(CreateIssueRequest.class)))
            .thenThrow(new ProjectNotFoundException(projectId));

        mockMvc.perform(post("/api/projects/{projectId}/issues", projectId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "TestDescription",
                    "status": "OPEN",
                    "priority": "LOW"
                }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Project not found with id: " + projectId));
    }

    @Test
    void createIssue_returns400_whenTitleIsEmpty() throws Exception {
        mockMvc.perform(post("/api/projects/1/issues")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "",
                    "description": "TestDescription",
                    "status": "OPEN",
                    "priority": "LOW"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.title").value("must not be blank"));

        verifyNoInteractions(issueService);
    }

    @Test
    void createIssue_returns400_whenTitleIsTooLong() throws Exception {
        String titleTooLong = "a".repeat(151);

        mockMvc.perform(post("/api/projects/1/issues")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "%s",
                    "description": "TestDescription",
                    "status": "OPEN",
                    "priority": "LOW"
                }
                """.formatted(titleTooLong)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.title").value("size must be between 0 and 150"));

        verifyNoInteractions(issueService);
    }

    @Test
    void createIssue_returns400_whenDescriptionIsTooLong() throws Exception {
        String descriptionTooLong = "a".repeat(1001);

        mockMvc.perform(post("/api/projects/1/issues")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "%s",
                    "status": "OPEN",
                    "priority": "LOW"
                }
                """.formatted(descriptionTooLong)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.description").value("size must be between 0 and 1000"));

        verifyNoInteractions(issueService);
    }

    @Test
    void createIssue_returns400_whenStatusIsMissing() throws Exception {
        mockMvc.perform(post("/api/projects/1/issues")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "TestDescription",
                    "priority": "LOW"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.status").value("must not be null"));

        verifyNoInteractions(issueService);
    }

    @Test
    void createIssue_returns400_whenPriorityIsMissing() throws Exception {
        mockMvc.perform(post("/api/projects/1/issues")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "TestDescription",
                    "status": "OPEN"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.priority").value("must not be null"));

        verifyNoInteractions(issueService);
    }

    @Test
    void createIssue_returns400_whenStatusIsInvalid() throws Exception {
        mockMvc.perform(post("/api/projects/1/issues")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "TestDescription",
                    "status": "TODO",
                    "priority": "LOW"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid request body"));

        verifyNoInteractions(issueService);
    }

    @Test
    void createIssue_returns400_whenPriorityIsInvalid() throws Exception {
        mockMvc.perform(post("/api/projects/1/issues")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "TestDescription",
                    "status": "OPEN",
                    "priority": "EXTRAHIGH"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid request body"));

        verifyNoInteractions(issueService);
    }

    @Test
    void updateIssue_returns200_whenIssueExists() throws Exception {
        Long issueId = 1L;
        Long projectId = 2L;

        Project project = new Project("TestName", "TestDescription");
        ReflectionTestUtils.setField(project, "id", projectId);

        Issue issue = new Issue(
            "UpdatedTestTitle",
            "UpdatedTestDescription",
            IssueStatus.OPEN,
            IssuePriority.MEDIUM,
            project
        );

        when(issueService.update(eq(issueId), any(UpdateIssueRequest.class)))
            .thenReturn(issue);

        mockMvc.perform(put("/api/issues/{issueId}", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "UpdatedTestTitle",
                    "description": "UpdatedTestDescription",
                    "priority": "MEDIUM"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("UpdatedTestTitle"))
            .andExpect(jsonPath("$.description").value("UpdatedTestDescription"))
            .andExpect(jsonPath("$.priority").value("MEDIUM"));

        verify(issueService).update(eq(issueId), any(UpdateIssueRequest.class));
    }

    @Test
    void updateIssue_returns404_whenIssueDoesNotExist() throws Exception {
        Long issueId = 1L;

        when(issueService.update(eq(issueId), any(UpdateIssueRequest.class)))
            .thenThrow(new IssueNotFoundException(issueId));

        mockMvc.perform(put("/api/issues/{issueId}", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "UpdatedTestTitle",
                    "description": "UpdatedTestDescription",
                    "priority": "MEDIUM"
                }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Issue not found with id: " + issueId));
    }

    @Test
    void updateIssue_returns400_whenTitleIsEmpty() throws Exception {
        mockMvc.perform(put("/api/issues/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "",
                    "description": "TestDescription",
                    "priority": "LOW"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.title").value("must not be blank"));

        verifyNoInteractions(issueService);
    }

    @Test
    void updateIssue_returns400_whenTitleIsTooLong() throws Exception {
        String titleTooLong = "a".repeat(151);

        mockMvc.perform(put("/api/issues/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "%s",
                    "description": "TestDescription",
                    "priority": "LOW"
                }
                """.formatted(titleTooLong)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.title").value("size must be between 0 and 150"));

        verifyNoInteractions(issueService);
    }

    @Test
    void updateIssue_returns400_whenDescriptionIsTooLong() throws Exception {
        String descriptionTooLong = "a".repeat(1001);

        mockMvc.perform(put("/api/issues/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "%s",
                    "priority": "LOW"
                }
                """.formatted(descriptionTooLong)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.description").value("size must be between 0 and 1000"));

        verifyNoInteractions(issueService);
    }

    @Test
    void updateIssue_returns400_whenPriorityIsMissing() throws Exception {
        mockMvc.perform(put("/api/issues/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "TestDescription"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.priority").value("must not be null"));

        verifyNoInteractions(issueService);
    }

    @Test
    void updateIssue_returns400_whenPriorityIsInvalid() throws Exception {
        mockMvc.perform(put("/api/issues/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "TestDescription",
                    "priority": "EXTRAHIGH"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid request body"));

        verifyNoInteractions(issueService);
    }

    @Test
    void updateIssue_returns409_whenIssueIsClosed() throws Exception {
        Long issueId = 1L;

        when(issueService.update(eq(issueId), any(UpdateIssueRequest.class)))
            .thenThrow(new ClosedIssueUpdateException(issueId));

        mockMvc.perform(put("/api/issues/{issueId}", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "UpdatedTestTitle",
                    "description": "UpdatedTestDescription",
                    "priority": "MEDIUM"
                }
                """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                .value("Issue with id " + issueId + " is closed and cannot be updated."));

        verify(issueService).update(eq(issueId), any(UpdateIssueRequest.class));
    }

    @Test
    void deleteIssue_returns204_whenIssueExists() throws Exception {
        Long issueId = 1L;

        mockMvc.perform(delete("/api/issues/{issueId}", issueId))
            .andExpect(status().isNoContent());

        verify(issueService).delete(issueId);
    }

    @Test
    void deleteIssue_returns404_whenIssueDoesNotExist() throws Exception {
        Long issueId = 5L;

        doThrow(new IssueNotFoundException(issueId))
            .when(issueService)
            .delete(issueId);

        mockMvc.perform(delete("/api/issues/{issueId}", issueId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Issue not found with id: " + issueId));
    }
}
