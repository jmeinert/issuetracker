package de.jmeinert.issuetracker.project;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
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

    public Project create(String name, String description) {
        Project project = new Project(name, description);

        return projectRepository.save(project);
    }

    public Project update(Long id, String name, String description) {
        Project project = findById(id);
        project.update(name, description);

        return projectRepository.save(project);
    }

    public void delete(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ProjectNotFoundException(id);
        }

        projectRepository.deleteById(id);
    }
}
