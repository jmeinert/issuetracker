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
        Long id = 5L;

        when(projectRepository.findById(id))
            .thenReturn(Optional.empty());

        assertProjectNotFound(id, () -> projectService.findById(id));
    }

    @Test
    void create_savesProject() {
        String name = "TestName";
        String description = "TestDescription";

        projectService.create(name, description);
        verify(projectRepository).save(projectArgumentCaptor.capture());

        assertEquals(name, projectArgumentCaptor.getValue().getName());
        assertEquals(description, projectArgumentCaptor.getValue().getDescription());
    }

    @Test
    void update_updatesProject_whenProjectExists() {
        Long id = 1L;
        Project project = new Project("Testname", "TestDescription");
        String updatedName = "UpdatedTestName";
        String updatedDescription = "UpdatedTestDescription";

        when(projectRepository.findById(id))
            .thenReturn(Optional.of(project));

        Project updatedProject = projectService.update(id, updatedName, updatedDescription);

        verify(projectRepository).findById(id);

        assertEquals(updatedName, updatedProject.getName());
        assertEquals(updatedDescription, updatedProject.getDescription());
    }

    @Test
    void update_throwsProjectNotFoundException_whenProjectDoesNotExist() {
        Long id = 5L;

        when(projectRepository.findById(id))
            .thenReturn(Optional.empty());

        assertProjectNotFound(id, () -> projectService.update(id, "UpdatedTestName", "UpdatedTestDescription"));
    }

    @Test
    void delete_deletesProject_whenProjectExistsAndHasNoIssues() {
        Long id = 1L;
        Project project = new Project("Testname", "TestDescription");

        when(projectRepository.findById(id))
            .thenReturn(Optional.of(project));

        when(issueRepository.existsByProject(project))
            .thenReturn(false);

        projectService.delete(id);

        verify(projectRepository).findById(id);
        verify(projectRepository).delete(project);
    }

    @Test
    void delete_throwsProjectNotFoundException_whenProjectDoesNotExist() {
        Long id = 5L;

        when(projectRepository.findById(id))
            .thenReturn(Optional.empty());

        assertProjectNotFound(id, () -> projectService.delete(id));

        verify(projectRepository).findById(id);
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(issueRepository);
    }

    @Test
    void delete_throwsProjectHasIssuesException_whenProjectHasIssues() {
        Long id = 1L;
        Project project = new Project("Testname", "TestDescription");

        when(projectRepository.findById(id))
            .thenReturn(Optional.of(project));

        when(issueRepository.existsByProject(project))
            .thenReturn(true);

        ProjectHasIssuesException exception = assertThrows(
            ProjectHasIssuesException.class,
            () -> projectService.delete(id)
        );
        assertEquals(
            "Project with id " + id + " cannot be deleted because it still contains issues.",
            exception.getMessage()
        );

        verify(projectRepository).findById(id);
        verifyNoMoreInteractions(projectRepository);
    }

    private void assertProjectNotFound(Long id, Executable executable) {
        ProjectNotFoundException exception = assertThrows(
            ProjectNotFoundException.class,
            executable
        );
        assertEquals("Project not found with id: " + id, exception.getMessage());
    }
}
