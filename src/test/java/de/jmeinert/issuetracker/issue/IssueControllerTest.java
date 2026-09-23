package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.BaseSecurityWebMvcTest;
import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectNotFoundException;
import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserDisabledException;
import de.jmeinert.issuetracker.user.UserNotFoundException;
import de.jmeinert.issuetracker.user.UserTestBuilder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IssueController.class)
@WithMockUser(roles = "ADMIN")
class IssueControllerTest extends BaseSecurityWebMvcTest {

    @MockitoBean
    private IssueService issueService;

    private final Project project = new Project("TestName", "TestDescription");

    private final User reporter = new UserTestBuilder()
        .username("reporter")
        .email("reporter@example.com")
        .build();

    @Test
    void getIssues_returns200_whenPaginationParametersAreNotProvided() throws Exception {
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );
        Page<Issue> emptyPage = Page.empty(pageable);

        when(issueService.findAll(IssueFilter.empty(), pageable))
            .thenReturn(emptyPage);

        mockMvc.perform(get("/api/issues"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void getIssues_returns200_whenCustomPaginationAndSortingAreRequested() throws Exception {
        Long projectId = 1L;
        ReflectionTestUtils.setField(project, "id", projectId);

        List<Issue> issues = List.of(
            new IssueTestBuilder(project, reporter).build(),
            new IssueTestBuilder(project, reporter)
                .title("TestTitle2")
                .description("TestDescription2")
                .build()
        );
        Pageable pageable = PageRequest.of(
            1,
            1,
            Sort.by(Sort.Order.asc("title"))
        );
        Page<Issue> issuePage = new PageImpl<>(
            List.of(issues.get(1)),
            pageable,
            issues.size()
        );

        when(issueService.findAll(IssueFilter.empty(), pageable))
            .thenReturn(issuePage);

        mockMvc.perform(get("/api/issues?page=1&size=1&sort=title,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("TestTitle2"))
            .andExpect(jsonPath("$.content[0].description").value("TestDescription2"))
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getIssues_returns200WithCappedPageSize_whenRequestedSizeExceedsMaximum() throws Exception {
        Pageable pageable = PageRequest.of(
            0,
            100,
            Sort.by(Sort.Order.desc("createdAt"))
        );
        Page<Issue> emptyPage = Page.empty(pageable);

        when(issueService.findAll(IssueFilter.empty(), pageable))
            .thenReturn(emptyPage);

        mockMvc.perform(get("/api/issues?size=101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void getIssues_returns200_whenFilterParametersAreProvided() throws Exception {
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );
        Page<Issue> emptyPage = Page.empty(pageable);
        IssueFilter filter = new IssueFilter(1L, IssueStatus.OPEN, IssuePriority.LOW, "test");

        when(issueService.findAll(filter, pageable))
            .thenReturn(emptyPage);

        mockMvc.perform(get("/api/issues")
                .param("projectId", "1")
                .param("status", "OPEN")
                .param("priority", "LOW")
                .param("search", "test"))
            .andExpect(status().isOk());
    }

    @Test
    void getIssues_returns400_whenSortFieldIsUnsupported() throws Exception {
        String invalidSortField = "priority";
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.asc(invalidSortField))
        );

        InvalidSortFieldException exception = new InvalidSortFieldException(
            invalidSortField,
            List.of("createdAt", "updatedAt", "title")
        );

        when(issueService.findAll(IssueFilter.empty(), pageable))
            .thenThrow(exception);

        mockMvc.perform(get("/api/issues?sort=" + invalidSortField + ",asc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(exception.getMessage()));
    }

    @Test
    void getIssues_returns400_whenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/issues?status=FINISHED"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Invalid value 'FINISHED' for argument 'status'."));

        verifyNoInteractions(issueService);
    }

    @Test
    void getIssues_returns400_whenSearchTextIsTooLong() throws Exception {
        String tooLongSearchText = "a".repeat(101);

        mockMvc.perform(get("/api/issues?search=" + tooLongSearchText))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.search").value("size must be between 0 and 100"));

        verifyNoInteractions(issueService);
    }

    @Test
    @WithAnonymousUser
    void getIssues_returns401_whenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/issues")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Authentication required"))
            .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE));

        verifyNoInteractions(issueService);
    }

    @Test
    void getIssueById_returns200_whenIssueExists() throws Exception {
        Long issueId = 1L;
        Long projectId = 2L;

        ReflectionTestUtils.setField(project, "id", projectId);

        Issue issue = new IssueTestBuilder(project, reporter).build();

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
        ReflectionTestUtils.setField(project, "id", projectId);

        List<Issue> issues = List.of(
            new IssueTestBuilder(project, reporter).build(),
            new IssueTestBuilder(project, reporter)
                .title("TestTitle2")
                .description("TestDescription2")
                .build()
        );
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Issue> issuePage = new PageImpl<>(
            issues,
            pageable,
            issues.size()
        );

        when(issueService.findAllByProjectId(projectId, pageable))
            .thenReturn(issuePage);

        mockMvc.perform(get("/api/projects/{projectId}/issues", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("TestTitle"))
            .andExpect(jsonPath("$.content[0].description").value("TestDescription"))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getIssuesByProjectId_returns404_whenProjectDoesNotExist() throws Exception {
        Long projectId = 5L;

        when(issueService.findAllByProjectId(eq(projectId), any(Pageable.class)))
            .thenThrow(new ProjectNotFoundException(projectId));

        mockMvc.perform(get("/api/projects/{projectId}/issues", projectId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Project not found with id: " + projectId));
    }

    @Test
    void createIssue_returns201_whenRequestIsValid() throws Exception {
        String title = "TestTitle";
        String description = "TestDescription";
        IssuePriority priority = IssuePriority.LOW;
        Long projectId = 1L;

        ReflectionTestUtils.setField(project, "id", projectId);

        CreateIssueRequest request = new CreateIssueRequest(
            title,
            description,
            priority
        );

        Issue issue = new IssueTestBuilder(project, reporter).build();

        when(issueService.create(projectId, request))
            .thenReturn(issue);

        mockMvc.perform(post("/api/projects/{projectId}/issues", projectId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "%s",
                    "description": "%s",
                    "priority": "%s"
                }
                """.formatted(title, description, priority)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value(title))
            .andExpect(jsonPath("$.description").value(description))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.priority").value(priority.name()))
            .andExpect(jsonPath("$.projectId").value(projectId));

        verify(issueService).create(projectId, request);
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
                    "priority": "LOW"
                }
                """.formatted(descriptionTooLong)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.description").value("size must be between 0 and 1000"));

        verifyNoInteractions(issueService);
    }

    @Test
    void createIssue_returns400_whenPriorityIsMissing() throws Exception {
        mockMvc.perform(post("/api/projects/1/issues")
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
    void createIssue_returns400_whenPriorityIsInvalid() throws Exception {
        mockMvc.perform(post("/api/projects/1/issues")
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
    void updateIssue_returns200_whenIssueExists() throws Exception {
        Long issueId = 1L;
        Long projectId = 2L;
        String title = "UpdatedTestTitle";
        String description = "UpdatedTestDescription";
        IssuePriority priority = IssuePriority.MEDIUM;

        ReflectionTestUtils.setField(project, "id", projectId);

        UpdateIssueRequest request = new UpdateIssueRequest(title, description, priority);

        Issue issue = new IssueTestBuilder(project, reporter)
            .title(title)
            .description(description)
            .priority(priority)
            .build();

        when(issueService.update(issueId, request))
            .thenReturn(issue);

        mockMvc.perform(put("/api/issues/{issueId}", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "%s",
                    "description": "%s",
                    "priority": "%s"
                }
                """.formatted(title, description, priority)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value(title))
            .andExpect(jsonPath("$.description").value(description))
            .andExpect(jsonPath("$.priority").value(priority.name()));

        verify(issueService).update(issueId, request);
    }

    @Test
    void updateIssue_returns404_whenIssueDoesNotExist() throws Exception {
        Long issueId = 1L;

        UpdateIssueRequest request = new UpdateIssueRequest(
            "UpdatedTestTitle",
            "UpdatedTestDescription",
            IssuePriority.MEDIUM
        );

        when(issueService.update(issueId, request))
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

        UpdateIssueRequest request = new UpdateIssueRequest(
            "UpdatedTestTitle",
            "UpdatedTestDescription",
            IssuePriority.MEDIUM
        );

        when(issueService.update(issueId, request))
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

        verify(issueService).update(issueId, request);
    }

    @Test
    void changeIssueStatus_returns200_whenIssueExistsAndTransitionIsAllowed() throws Exception {
        Long issueId = 1L;
        Long projectId = 2L;
        IssueStatus status = IssueStatus.IN_PROGRESS;

        ReflectionTestUtils.setField(project, "id", projectId);

        Issue issue = new IssueTestBuilder(project, reporter)
            .status(status)
            .build();

        when(issueService.changeStatus(issueId, status))
            .thenReturn(issue);

        mockMvc.perform(patch("/api/issues/{issueId}/status", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "status": "IN_PROGRESS"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(status.name()));

        verify(issueService).changeStatus(issueId, status);
    }

    @Test
    void changeIssueStatus_returns404_whenIssueDoesNotExist() throws Exception {
        Long issueId = 1L;

        when(issueService.changeStatus(issueId, IssueStatus.IN_PROGRESS))
            .thenThrow(new IssueNotFoundException(issueId));

        mockMvc.perform(patch("/api/issues/{issueId}/status", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "status": "IN_PROGRESS"
                }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Issue not found with id: " + issueId));
    }

    @Test
    void changeIssueStatus_returns409_whenTransitionIsInvalid() throws Exception {
        Long issueId = 1L;
        IssueStatus currentStatus = IssueStatus.OPEN;
        IssueStatus targetStatus = IssueStatus.OPEN;
        List<IssueStatus> allowedStatuses = List.of(IssueStatus.IN_PROGRESS);

        InvalidIssueStatusTransitionException exception = new InvalidIssueStatusTransitionException(
            issueId,
            currentStatus,
            targetStatus,
            allowedStatuses
        );

        when(issueService.changeStatus(issueId, targetStatus))
            .thenThrow(exception);

        mockMvc.perform(patch("/api/issues/{issueId}/status", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "status": "OPEN"
                }
                """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(exception.getMessage()));

        verify(issueService).changeStatus(issueId, targetStatus);
    }

    @Test
    void changeIssueStatus_returns400_whenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/issues/1/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.status").value("must not be null"));

        verifyNoInteractions(issueService);
    }

    @Test
    void changeIssueStatus_returns400_whenStatusIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/issues/1/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "status": "WAITING"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid request body"));

        verifyNoInteractions(issueService);
    }

    @Test
    void assignIssue_returns200_whenAssigneeExists() throws Exception {
        Long issueId = 1L;
        Long assigneeId = 2L;

        User assignee = new UserTestBuilder()
            .id(assigneeId)
            .username("assignee")
            .email("assignee@example.com")
            .build();

        Issue issue = new IssueTestBuilder(project, reporter)
            .assignee(assignee)
            .build();

        when(issueService.assign(issueId, assigneeId))
            .thenReturn(issue);

        mockMvc.perform(patch("/api/issues/{issueId}/assignee", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "assigneeId": %d
                }
                """.formatted(assigneeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assigneeId").value(assigneeId));
    }

    @Test
    void assignIssue_returns400_whenAssigneeIdIsMissing() throws Exception {
        mockMvc.perform(patch("/api/issues/1/assignee")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.assigneeId").value("must not be null"));

        verifyNoInteractions(issueService);
    }

    @Test
    void assignIssue_returns404_whenIssueDoesNotExist() throws Exception {
        Long issueId = 5L;
        Long assigneeId = 2L;

        when(issueService.assign(issueId, assigneeId))
            .thenThrow(new IssueNotFoundException(issueId));

        mockMvc.perform(patch("/api/issues/{issueId}/assignee", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "assigneeId": %d
                }
                """.formatted(assigneeId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Issue not found with id: " + issueId));
    }

    @Test
    void assignIssue_returns404_whenAssigneeDoesNotExist() throws Exception {
        Long issueId = 1L;
        Long assigneeId = 5L;

        when(issueService.assign(issueId, assigneeId))
            .thenThrow(new UserNotFoundException(assigneeId));

        mockMvc.perform(patch("/api/issues/{issueId}/assignee", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "assigneeId": %d
                }
                """.formatted(assigneeId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found with id: " + assigneeId));
    }

    @Test
    void assignIssue_returns409_whenAssigneeIsDisabled() throws Exception {
        Long issueId = 1L;
        Long assigneeId = 2L;

        when(issueService.assign(issueId, assigneeId))
            .thenThrow(new UserDisabledException());

        mockMvc.perform(patch("/api/issues/{issueId}/assignee", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "assigneeId": %d
                }
                """.formatted(assigneeId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("User is disabled"));
    }

    @Test
    void unassignIssue_returns200_whenIssueExists() throws Exception {
        Long issueId = 1L;

        Issue issue = new IssueTestBuilder(project, reporter).build();

        when(issueService.unassign(issueId))
            .thenReturn(issue);

        mockMvc.perform(delete("/api/issues/{issueId}/assignee", issueId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assigneeId").isEmpty());
    }

    @Test
    void unassignIssue_returns404_whenIssueDoesNotExist() throws Exception {
        Long issueId = 5L;

        when(issueService.unassign(issueId))
            .thenThrow(new IssueNotFoundException(issueId));

        mockMvc.perform(delete("/api/issues/{issueId}/assignee", issueId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Issue not found with id: " + issueId));
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
