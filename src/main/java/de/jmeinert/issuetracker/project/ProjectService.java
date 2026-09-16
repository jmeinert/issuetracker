package de.jmeinert.issuetracker.project;

import de.jmeinert.issuetracker.issue.IssueRepository;
import de.jmeinert.issuetracker.security.IsAdmin;

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

    public Project findById(Long projectId) {
        return projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    @Transactional
    @IsAdmin
    public Project create(String name, String description) {
        Project project = new Project(name, description);

        return projectRepository.save(project);
    }

    @Transactional
    @IsAdmin
    public Project update(Long projectId, String name, String description) {
        Project project = findById(projectId);
        project.updateDetails(name, description);
        return project;
    }

    @Transactional
    @IsAdmin
    public void delete(Long projectId) {
        Project project = findById(projectId);

        if (issueRepository.existsByProject(project)) {
            throw new ProjectHasIssuesException(projectId);
        }

        projectRepository.delete(project);
    }
}
