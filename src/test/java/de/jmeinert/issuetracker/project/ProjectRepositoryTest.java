package de.jmeinert.issuetracker.project;

import de.jmeinert.issuetracker.config.PersistenceConfig;
import de.jmeinert.issuetracker.config.TestcontainersConfiguration;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, PersistenceConfig.class})
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void save_persistsProject() {
        Project project = new Project("TestName", "TestDescription");

        Project savedProject = projectRepository.saveAndFlush(project);
        Long savedProjectId = savedProject.getId();
        assertNotNull(savedProjectId);

        entityManager.clear();

        Project persistedProject = projectRepository.findById(savedProjectId)
            .orElseThrow();

        assertEquals(savedProjectId, persistedProject.getId());
        assertEquals("TestName", persistedProject.getName());
        assertEquals("TestDescription", persistedProject.getDescription());
        assertNotNull(persistedProject.getCreatedAt());
        assertNotNull(persistedProject.getUpdatedAt());
    }
}
