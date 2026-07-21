package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IssueService {

    private final IssueRepository issueRepository;

    private final ProjectService projectService;

    public IssueService(
        IssueRepository issueRepository,
        ProjectService projectService
    ) {
        this.issueRepository = issueRepository;
        this.projectService = projectService;
    }

    public Issue findById(Long id) {
        return issueRepository.findById(id)
            .orElseThrow(() -> new IssueNotFoundException(id));
    }

    @Transactional
    public Issue create(Long projectId, CreateIssueRequest request) {
        Project project = projectService.findById(projectId);
        Issue issue = new Issue(
            request.title(),
            request.description(),
            request.status(),
            request.priority(),
            project
        );

        return issueRepository.save(issue);
    }
}
