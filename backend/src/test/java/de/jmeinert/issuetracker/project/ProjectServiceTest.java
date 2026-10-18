package de.jmeinert.issuetracker.project;

import de.jmeinert.issuetracker.issue.IssueRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private ProjectService projectService;

    @Captor
    private ArgumentCaptor<Project> projectArgumentCaptor;

    @Test
    void findById_returnsProject_whenProjectExists() {
        Project project = new Project("TestName", "TestDescription");

        when(projectRepository.findById(1L))
            .thenReturn(Optional.of(project));

        assertEquals(project, projectService.findById(1L));
    }

    @Test
    void findById_throwsProjectNotFoundException_whenProjectDoesNotExist() {
        Long projectId = 5L;

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.empty());

        assertProjectNotFound(projectId, () -> projectService.findById(projectId));
    }

    @Test
    void create_savesProject() {
        String name = "TestName";
        String description = "TestDescription";

        ProjectRequest request = new ProjectRequest(name, description);

        projectService.create(request);
        verify(projectRepository).save(projectArgumentCaptor.capture());

        assertEquals(name, projectArgumentCaptor.getValue().getName());
        assertEquals(description, projectArgumentCaptor.getValue().getDescription());
    }

    @Test
    void update_updatesProject_whenProjectExists() {
        Long projectId = 1L;
        Project project = new Project("Testname", "TestDescription");
        String updatedName = "UpdatedTestName";
        String updatedDescription = "UpdatedTestDescription";

        ProjectRequest request = new ProjectRequest(updatedName, updatedDescription);

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.of(project));

        Project updatedProject = projectService.update(projectId, request);

        verify(projectRepository).findById(projectId);

        assertEquals(updatedName, updatedProject.getName());
        assertEquals(updatedDescription, updatedProject.getDescription());
    }

    @Test
    void update_throwsProjectNotFoundException_whenProjectDoesNotExist() {
        Long projectId = 5L;

        ProjectRequest request = new ProjectRequest(
            "UpdatedTestName",
            "UpdatedTestDescription"
        );

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.empty());

        assertProjectNotFound(projectId, () -> projectService.update(projectId, request));
    }

    @Test
    void delete_deletesProject_whenProjectExistsAndHasNoIssues() {
        Long projectId = 1L;
        Project project = new Project("Testname", "TestDescription");

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.of(project));

        when(issueRepository.existsByProject(project))
            .thenReturn(false);

        projectService.delete(projectId);

        verify(projectRepository).findById(projectId);
        verify(projectRepository).delete(project);
    }

    @Test
    void delete_throwsProjectNotFoundException_whenProjectDoesNotExist() {
        Long projectId = 5L;

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.empty());

        assertProjectNotFound(projectId, () -> projectService.delete(projectId));

        verify(projectRepository).findById(projectId);
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(issueRepository);
    }

    @Test
    void delete_throwsProjectHasIssuesException_whenProjectHasIssues() {
        Long projectId = 1L;
        Project project = new Project("Testname", "TestDescription");

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.of(project));

        when(issueRepository.existsByProject(project))
            .thenReturn(true);

        ProjectHasIssuesException exception = assertThrows(
            ProjectHasIssuesException.class,
            () -> projectService.delete(projectId)
        );
        assertEquals(
            "Project with id " + projectId + " cannot be deleted because it still contains issues.",
            exception.getMessage()
        );

        verify(projectRepository).findById(projectId);
        verifyNoMoreInteractions(projectRepository);
    }

    private void assertProjectNotFound(Long projectId, Executable executable) {
        ProjectNotFoundException exception = assertThrows(
            ProjectNotFoundException.class,
            executable
        );
        assertEquals("Project not found with id: " + projectId, exception.getMessage());
    }
}
