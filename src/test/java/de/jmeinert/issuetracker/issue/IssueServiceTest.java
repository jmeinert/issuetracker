package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectNotFoundException;
import de.jmeinert.issuetracker.project.ProjectService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @InjectMocks
    private IssueService issueService;

    @Captor
    private ArgumentCaptor<Issue> issueArgumentCaptor;

    @Test
    void findAllByProjectId_returnsAllIssuesOfProject_whenProjectExists() {
        Long projectId = 1L;
        Project project = new Project("TestName", "TestDescription");
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

        when(projectService.findById(projectId))
            .thenReturn(project);

        when(issueRepository.findAllByProject(project))
            .thenReturn(issues);

        assertEquals(issues, issueService.findAllByProjectId(projectId));
    }

    @Test
    void findAllByProjectId_returnsEmptyList_whenProjectExistsAndHasNoIssues() {
        Long projectId = 1L;
        Project project = new Project("TestName", "TestDescription");

        when(projectService.findById(projectId))
            .thenReturn(project);

        when(issueRepository.findAllByProject(project))
            .thenReturn(List.of());

        assertEquals(List.of(), issueService.findAllByProjectId(projectId));
    }

    @Test
    void findAllByProjectId_throwsProjectNotFoundException_whenProjectDoesNotExist() {
        Long projectId = 5L;

        when(projectService.findById(projectId))
            .thenThrow(new ProjectNotFoundException(projectId));

        assertProjectNotFound(projectId, () -> issueService.findAllByProjectId(projectId));

        verifyNoInteractions(issueRepository);
    }

    @Test
    void findById_returnsIssue_whenIssueExists() {
        Long issueId = 1L;
        Project project = new Project("TestName", "TestDescription");
        Issue issue = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );

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
        CreateIssueRequest request = new CreateIssueRequest(
            "TestTitle",
            "TestDescription",
            IssuePriority.LOW
        );
        Project project = new Project("TestName", "TestDescription");

        when(projectService.findById(projectId))
            .thenReturn(project);

        issueService.create(projectId, request);
        verify(issueRepository).save(issueArgumentCaptor.capture());

        Issue savedIssue = issueArgumentCaptor.getValue();

        assertEquals("TestTitle", savedIssue.getTitle());
        assertEquals("TestDescription", savedIssue.getDescription());
        assertEquals(IssueStatus.OPEN, savedIssue.getStatus());
        assertEquals(IssuePriority.LOW, savedIssue.getPriority());
        assertEquals(project, savedIssue.getProject());
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
        Project project = new Project("TestName", "TestDescription");
        Issue issue = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );
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
        Project project = new Project("TestName", "TestDescription");
        Issue issue = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.CLOSED,
            IssuePriority.LOW,
            project
        );
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

    @Test
    void delete_deletesIssue_whenIssueExists() {
        Long issueId = 1L;
        Project project = new Project("TestName", "TestDescription");
        Issue issue = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );

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
