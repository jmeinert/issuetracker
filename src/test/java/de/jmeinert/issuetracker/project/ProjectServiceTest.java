package de.jmeinert.issuetracker.project;

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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

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
    void update_updatesAndSavesProject_whenProjectExists() {
        Project project = new Project("Testname", "TestDescription");
        String updatedName = "UpdatedTestName";
        String updatedDescription = "UpdatedTestDescription";

        when(projectRepository.findById(1L))
            .thenReturn(Optional.of(project));

        projectService.update(1L, updatedName, updatedDescription);
        verify(projectRepository).save(projectArgumentCaptor.capture());

        assertEquals(updatedName, projectArgumentCaptor.getValue().getName());
        assertEquals(updatedDescription, projectArgumentCaptor.getValue().getDescription());
    }

    @Test
    void update_throwsProjectNotFoundException_whenProjectDoesNotExist() {
        Long id = 5L;

        when(projectRepository.findById(id))
            .thenReturn(Optional.empty());

        assertProjectNotFound(id, () -> projectService.update(id, "UpdatedTestName", "UpdatedTestDescription"));
    }

    @Test
    void delete_deletesProject_whenProjectExists() {
        Long id = 1L;

        when(projectRepository.existsById(id))
            .thenReturn(true);

        projectService.delete(id);
        verify(projectRepository).deleteById(id);
    }

    @Test
    void delete_throwsProjectNotFoundException_whenProjectDoesNotExist() {
        Long id = 5L;

        when(projectRepository.existsById(id))
            .thenReturn(false);

        assertProjectNotFound(id, () -> projectService.delete(id));

        verify(projectRepository).existsById(id);
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
