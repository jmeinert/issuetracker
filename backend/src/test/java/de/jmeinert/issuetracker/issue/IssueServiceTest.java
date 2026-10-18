package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectNotFoundException;
import de.jmeinert.issuetracker.project.ProjectService;
import de.jmeinert.issuetracker.security.AuthenticatedUserProvider;
import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserDisabledException;
import de.jmeinert.issuetracker.user.UserNotFoundException;
import de.jmeinert.issuetracker.user.UserService;
import de.jmeinert.issuetracker.user.UserTestBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    private IssueService issueService;

    @Captor
    private ArgumentCaptor<Issue> issueArgumentCaptor;

    private final Project project = new Project("TestName", "TestDescription");

    private final User reporter = new UserTestBuilder()
        .username("reporter")
        .email("reporter@example.com")
        .build();

    @ParameterizedTest
    @ValueSource(strings = {"createdAt", "updatedAt", "title"})
    void findAll_returnsPage_whenSortFieldIsSupported(String sortField) {
        Pageable requestedPageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc(sortField))
        );
        Pageable validatedPageable = PageRequest.of(
            0,
            20,
            Sort.by(
                Sort.Order.desc(sortField),
                Sort.Order.asc("id")
            )
        );
        Page<Issue> expectedPage = Page.empty(validatedPageable);

        when(issueRepository.findAll(Specification.unrestricted(), validatedPageable))
            .thenReturn(expectedPage);

        assertEquals(expectedPage, issueService.findAll(IssueFilter.empty(), requestedPageable));
    }

    @Test
    void findAll_throwsInvalidSortFieldException_whenSortFieldIsUnsupported() {
        String invalidSortField = "priority";
        Pageable requestedPageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc(invalidSortField))
        );

        InvalidSortFieldException exception = assertThrows(
            InvalidSortFieldException.class,
            () -> issueService.findAll(IssueFilter.empty(), requestedPageable)
        );
        assertEquals(
            "Sort field '" + invalidSortField + "' is invalid. Allowed sort fields: createdAt, updatedAt, title.",
            exception.getMessage()
        );

        verifyNoInteractions(issueRepository);
    }

    @Test
    void findAllByProjectId_returnsPageOfIssuesOfProject_whenProjectExists() {
        Long projectId = 1L;
        List<Issue> issues = List.of(
            new IssueTestBuilder(project, reporter).build(),
            new IssueTestBuilder(project, reporter)
                .title("TestTitle2")
                .description("TestDescription2")
                .build()
        );
        Pageable requestedPageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );
        Pageable validatedPageable = PageRequest.of(
            0,
            20,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.asc("id")
            )
        );
        Page<Issue> issuePage = new PageImpl<>(
            issues,
            validatedPageable,
            issues.size()
        );

        when(projectService.findById(projectId))
            .thenReturn(project);

        when(issueRepository.findAllByProject(project, validatedPageable))
            .thenReturn(issuePage);

        assertEquals(issuePage, issueService.findAllByProjectId(projectId, requestedPageable));
    }

    @Test
    void findAllByProjectId_throwsProjectNotFoundException_whenProjectDoesNotExist() {
        Long projectId = 5L;
        Pageable requestedPageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        when(projectService.findById(projectId))
            .thenThrow(new ProjectNotFoundException(projectId));

        assertProjectNotFound(projectId, () -> issueService.findAllByProjectId(projectId, requestedPageable));

        verifyNoInteractions(issueRepository);
    }

    @Test
    void findById_returnsIssue_whenIssueExists() {
        Long issueId = 1L;
        Issue issue = new IssueTestBuilder(project, reporter).build();

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.of(issue));

        assertEquals(issue, issueService.findById(issueId));
    }

    @Test
    void findById_throwsIssueNotFoundException_whenIssueDoesNotExist() {
        Long issueId = 5L;

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.empty());

        assertIssueNotFound(issueId, () -> issueService.findById(issueId));
    }

    @Test
    void create_savesIssue() {
        Long projectId = 1L;
        Long reporterId = 2L;
        CreateIssueRequest request = new CreateIssueRequest(
            "TestTitle",
            "TestDescription",
            IssuePriority.LOW
        );

        when(projectService.findById(projectId))
            .thenReturn(project);

        when(authenticatedUserProvider.getUserId())
            .thenReturn(reporterId);

        when(userService.findById(reporterId))
            .thenReturn(reporter);

        issueService.create(projectId, request);
        verify(issueRepository).save(issueArgumentCaptor.capture());

        Issue savedIssue = issueArgumentCaptor.getValue();

        assertEquals("TestTitle", savedIssue.getTitle());
        assertEquals("TestDescription", savedIssue.getDescription());
        assertEquals(IssueStatus.OPEN, savedIssue.getStatus());
        assertEquals(IssuePriority.LOW, savedIssue.getPriority());
        assertEquals(project, savedIssue.getProject());
        assertEquals(reporter, savedIssue.getReporter());
    }

    @Test
    void create_throwsProjectNotFoundException_whenProjectDoesNotExist() {
        Long projectId = 5L;
        CreateIssueRequest request = new CreateIssueRequest(
            "TestTitle",
            "TestDescription",
            IssuePriority.LOW
        );

        when(projectService.findById(projectId))
            .thenThrow(new ProjectNotFoundException(projectId));

        assertProjectNotFound(projectId, () -> issueService.create(projectId, request));

        verifyNoInteractions(issueRepository);
    }

    @Test
    void update_updatesIssue_whenIssueExists() {
        Long issueId = 1L;
        Issue issue = new IssueTestBuilder(project, reporter).build();
        UpdateIssueRequest request = new UpdateIssueRequest(
            "UpdatedTestTitle",
            "UpdatedTestDescription",
            IssuePriority.MEDIUM
        );

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.of(issue));

        Issue updatedIssue = issueService.update(issueId, request);

        verify(issueRepository).findById(issueId);

        assertEquals(request.title(), updatedIssue.getTitle());
        assertEquals(request.description(), updatedIssue.getDescription());
        assertEquals(request.priority(), updatedIssue.getPriority());
    }

    @Test
    void update_throwsIssueNotFoundException_whenIssueDoesNotExist() {
        Long issueId = 5L;
        UpdateIssueRequest request = new UpdateIssueRequest(
            "UpdatedTestTitle",
            "UpdatedTestDescription",
            IssuePriority.MEDIUM
        );

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.empty());

        assertIssueNotFound(issueId, () -> issueService.update(issueId, request));
    }

    @Test
    void update_throwsClosedIssueUpdateException_whenIssueIsClosed() {
        Long issueId = 1L;
        Issue issue = new IssueTestBuilder(project, reporter)
            .status(IssueStatus.CLOSED)
            .build();
        UpdateIssueRequest request = new UpdateIssueRequest(
            "UpdatedTestTitle",
            "UpdatedTestDescription",
            IssuePriority.MEDIUM
        );

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.of(issue));

        ClosedIssueUpdateException exception = assertThrows(
            ClosedIssueUpdateException.class,
            () -> issueService.update(issueId, request)
        );

        assertEquals(
            "Issue with id " + issueId + " is closed and cannot be updated.",
            exception.getMessage()
        );
        assertEquals("TestTitle", issue.getTitle());
        assertEquals("TestDescription", issue.getDescription());
        assertEquals(IssuePriority.LOW, issue.getPriority());
    }

    @ParameterizedTest
    @CsvSource({
        "OPEN,        IN_PROGRESS",
        "IN_PROGRESS, RESOLVED",
        "IN_PROGRESS, CLOSED",
        "RESOLVED,    IN_PROGRESS",
        "RESOLVED,    CLOSED",
        "CLOSED,      OPEN"
    })
    void changeStatus_changesStatus_whenTransitionIsAllowed(
        IssueStatus currentStatus,
        IssueStatus targetStatus
    ) {
        Long issueId = 1L;
        Issue issue = new IssueTestBuilder(project, reporter)
            .status(currentStatus)
            .build();

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.of(issue));

        Issue changedIssue = issueService.changeStatus(issueId, targetStatus);

        assertEquals(targetStatus, changedIssue.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
        "OPEN,        OPEN",
        "OPEN,        RESOLVED",
        "OPEN,        CLOSED",
        "IN_PROGRESS, OPEN",
        "IN_PROGRESS, IN_PROGRESS",
        "RESOLVED,    OPEN",
        "RESOLVED,    RESOLVED",
        "CLOSED,      IN_PROGRESS",
        "CLOSED,      RESOLVED",
        "CLOSED,      CLOSED"
    })
    void changeStatus_throwsInvalidIssueStatusTransitionException_whenTransitionIsInvalid(
        IssueStatus currentStatus,
        IssueStatus targetStatus
    ) {
        Long issueId = 1L;
        Issue issue = new IssueTestBuilder(project, reporter)
            .status(currentStatus)
            .build();

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.of(issue));

        assertThrows(
            InvalidIssueStatusTransitionException.class,
            () -> issueService.changeStatus(issueId, targetStatus)
        );

        assertEquals(currentStatus, issue.getStatus());
    }

    @Test
    void changeStatus_throwsIssueNotFoundException_whenIssueDoesNotExist() {
        Long issueId = 5L;

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.empty());

        assertIssueNotFound(issueId, () -> issueService.changeStatus(issueId, IssueStatus.IN_PROGRESS));
    }

    @Test
    void assign_assignsUserToIssue_whenAssigneeExists() {
        Long issueId = 1L;
        Long assigneeId = 2L;

        Issue issue = new IssueTestBuilder(project, reporter).build();

        User assignee = new UserTestBuilder()
            .id(assigneeId)
            .username("assignee")
            .email("assignee@example.com")
            .enabled(true)
            .build();

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.of(issue));

        when(userService.findById(assigneeId))
            .thenReturn(assignee);

        Issue changedIssue = issueService.assign(issueId, assigneeId);

        assertEquals(assigneeId, changedIssue.getAssignee().getId());
    }

    @Test
    void assign_throwsIssueNotFoundException_whenIssueDoesNotExist() {
        Long issueId = 5L;
        Long assigneeId = 2L;

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.empty());

        assertIssueNotFound(issueId, () -> issueService.assign(issueId, assigneeId));
    }

    @Test
    void assign_throwsUserNotFoundException_whenAssigneeDoesNotExist() {
        Long issueId = 1L;
        Long assigneeId = 5L;

        Issue issue = new IssueTestBuilder(project, reporter).build();

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.of(issue));

        when(userService.findById(assigneeId))
            .thenThrow(new UserNotFoundException(assigneeId));

        UserNotFoundException exception = assertThrows(
            UserNotFoundException.class,
            () -> issueService.assign(issueId, assigneeId)
        );
        assertEquals("User not found with id: " + assigneeId, exception.getMessage());
    }

    @Test
    void assign_throwsUserDisabledException_whenAssigneeIsDisabled() {
        Long issueId = 1L;
        Long assigneeId = 2L;

        Issue issue = new IssueTestBuilder(project, reporter).build();

        User assignee = new UserTestBuilder()
            .username("assignee")
            .email("assignee@example.com")
            .enabled(false)
            .build();

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.of(issue));

        when(userService.findById(assigneeId))
            .thenReturn(assignee);

        UserDisabledException exception = assertThrows(
            UserDisabledException.class,
            () -> issueService.assign(issueId, assigneeId)
        );
        assertEquals("User is disabled", exception.getMessage());
    }

    @Test
    void unassign_clearsAssignee_whenIssueExists() {
        Long issueId = 1L;

        User assignee = new UserTestBuilder()
            .username("assignee")
            .email("assignee@example.com")
            .build();

        Issue issue = new IssueTestBuilder(project, reporter)
            .assignee(assignee)
            .build();

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.of(issue));

        Issue changedIssue = issueService.unassign(issueId);

        assertNull(changedIssue.getAssignee());
    }

    @Test
    void unassign_throwsIssueNotFoundException_whenIssueDoesNotExist() {
        Long issueId = 5L;

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.empty());

        assertIssueNotFound(issueId, () -> issueService.unassign(issueId));
    }

    @Test
    void delete_deletesIssue_whenIssueExists() {
        Long issueId = 1L;
        Issue issue = new IssueTestBuilder(project, reporter).build();

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.of(issue));

        issueService.delete(issueId);

        verify(issueRepository).findById(issueId);
        verify(issueRepository).delete(issue);
    }

    @Test
    void delete_throwsIssueNotFoundException_whenIssueDoesNotExist() {
        Long issueId = 5L;

        when(issueRepository.findById(issueId))
            .thenReturn(Optional.empty());

        assertIssueNotFound(issueId, () -> issueService.delete(issueId));

        verify(issueRepository).findById(issueId);
        verifyNoMoreInteractions(issueRepository);
    }

    private void assertIssueNotFound(Long issueId, Executable executable) {
        IssueNotFoundException exception = assertThrows(
            IssueNotFoundException.class,
            executable
        );
        assertEquals("Issue not found with id: " + issueId, exception.getMessage());
    }

    private void assertProjectNotFound(Long projectId, Executable executable) {
        ProjectNotFoundException exception = assertThrows(
            ProjectNotFoundException.class,
            executable
        );
        assertEquals("Project not found with id: " + projectId, exception.getMessage());
    }
}
