package de.jmeinert.issuetracker.project;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    public Project findById(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new ProjectNotFoundException(id));
    }

    @Transactional
    public Project create(String name, String description) {
        Project project = new Project(name, description);

        return projectRepository.save(project);
    }

    @Transactional
    public Project update(Long id, String name, String description) {
        Project project = findById(id);
        project.updateDetails(name, description);
        return project;
    }

    @Transactional
    public void delete(Long id) {
        Project project = findById(id);
        projectRepository.delete(project);
    }
}
