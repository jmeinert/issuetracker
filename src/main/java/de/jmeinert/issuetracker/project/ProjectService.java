package de.jmeinert.issuetracker.project;

import de.jmeinert.issuetracker.issue.IssueRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;

    private final IssueRepository issueRepository;

    public ProjectService(
        ProjectRepository projectRepository,
        IssueRepository issueRepository
    ) {
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
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

        if (issueRepository.existsByProject(project)) {
            throw new ProjectHasIssuesException(id);
        }

        projectRepository.delete(project);
    }
}
