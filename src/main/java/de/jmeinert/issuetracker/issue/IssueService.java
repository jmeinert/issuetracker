package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class IssueService {

    private static final Map<IssueStatus, List<IssueStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
        IssueStatus.OPEN, List.of(IssueStatus.IN_PROGRESS),
        IssueStatus.IN_PROGRESS, List.of(IssueStatus.RESOLVED, IssueStatus.CLOSED),
        IssueStatus.RESOLVED, List.of(IssueStatus.IN_PROGRESS, IssueStatus.CLOSED),
        IssueStatus.CLOSED, List.of(IssueStatus.OPEN)
    );

    private final IssueRepository issueRepository;

    private final ProjectService projectService;

    public IssueService(
        IssueRepository issueRepository,
        ProjectService projectService
    ) {
        this.issueRepository = issueRepository;
        this.projectService = projectService;
    }

    public List<Issue> findAllByProjectId(Long projectId) {
        Project project = projectService.findById(projectId);
        return issueRepository.findAllByProject(project);
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
            IssueStatus.OPEN,
            request.priority(),
            project
        );

        return issueRepository.save(issue);
    }

    @Transactional
    public Issue update(Long issueId, UpdateIssueRequest request) {
        Issue issue = findById(issueId);

        if (issue.getStatus() == IssueStatus.CLOSED) {
            throw new ClosedIssueUpdateException(issueId);
        }

        issue.updateDetails(request.title(), request.description(), request.priority());
        return issue;
    }

    @Transactional
    public Issue changeStatus(Long issueId, ChangeIssueStatusRequest request) {
        Issue issue = findById(issueId);
        List<IssueStatus> allowedStatuses = ALLOWED_STATUS_TRANSITIONS.get(issue.getStatus());
        IssueStatus targetStatus = request.status();

        if (!allowedStatuses.contains(targetStatus)) {
            throw new InvalidIssueStatusTransitionException(
                issueId,
                issue.getStatus(),
                targetStatus,
                allowedStatuses
            );
        }

        issue.changeStatusTo(targetStatus);
        return issue;
    }

    @Transactional
    public void delete(Long issueId) {
        Issue issue = findById(issueId);
        issueRepository.delete(issue);
    }
}
