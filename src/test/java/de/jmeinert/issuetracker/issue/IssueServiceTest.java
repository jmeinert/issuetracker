package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectNotFoundException;
import de.jmeinert.issuetracker.project.ProjectService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void create_savesIssue() {
        Long projectId = 1L;
        CreateIssueRequest request = new CreateIssueRequest(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
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
            IssueStatus.OPEN,
            IssuePriority.LOW
        );

        when(projectService.findById(projectId))
            .thenThrow(new ProjectNotFoundException(projectId));

        ProjectNotFoundException exception = assertThrows(
            ProjectNotFoundException.class,
            () -> issueService.create(projectId, request)
        );
        assertEquals("Project not found with id: " + projectId, exception.getMessage());

        verifyNoInteractions(issueRepository);
    }
}
