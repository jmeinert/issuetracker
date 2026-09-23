package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectService;
import de.jmeinert.issuetracker.security.AuthenticatedUserProvider;
import de.jmeinert.issuetracker.security.IsAdmin;
import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserDisabledException;
import de.jmeinert.issuetracker.user.UserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
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

    private static final List<String> ALLOWED_SORT_FIELDS = List.of("createdAt", "updatedAt", "title");

    private final IssueRepository issueRepository;

    private final ProjectService projectService;

    private final UserService userService;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public IssueService(
        IssueRepository issueRepository,
        ProjectService projectService,
        UserService userService,
        AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.issueRepository = issueRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public Page<Issue> findAll(IssueFilter filter, Pageable pageable) {
        Specification<Issue> specification = Specification.unrestricted();
        String searchText = filter.search();

        if (filter.projectId() != null) {
            specification = specification.and(IssueSpecifications.hasProjectId(filter.projectId()));
        }
        if (filter.status() != null) {
            specification = specification.and(IssueSpecifications.hasStatus(filter.status()));
        }
        if (filter.priority() != null) {
            specification = specification.and(IssueSpecifications.hasPriority(filter.priority()));
        }
        if (searchText != null && !searchText.isBlank()) {
            String normalizedSearchText = searchText.toLowerCase(Locale.ROOT).trim();
            specification = specification.and(IssueSpecifications.hasSearchText(normalizedSearchText));
        }

        return issueRepository.findAll(specification, getValidatedPageable(pageable));
    }

    public Page<Issue> findAllByProjectId(Long projectId, Pageable pageable) {
        Project project = projectService.findById(projectId);
        return issueRepository.findAllByProject(project, getValidatedPageable(pageable));
    }

    public Issue findById(Long issueId) {
        return issueRepository.findById(issueId)
            .orElseThrow(() -> new IssueNotFoundException(issueId));
    }

    @Transactional
    public Issue create(Long projectId, CreateIssueRequest request) {
        Project project = projectService.findById(projectId);
        User reporter = userService.findById(authenticatedUserProvider.getUserId());

        Issue issue = new Issue(
            request.title(),
            request.description(),
            IssueStatus.OPEN,
            request.priority(),
            project,
            reporter,
            null
        );

        return issueRepository.save(issue);
    }

    @Transactional
    @CanModifyIssue
    public Issue update(Long issueId, UpdateIssueRequest request) {
        Issue issue = findById(issueId);

        if (issue.getStatus() == IssueStatus.CLOSED) {
            throw new ClosedIssueUpdateException(issueId);
        }

        issue.updateDetails(request.title(), request.description(), request.priority());
        return issue;
    }

    @Transactional
    @CanModifyIssue
    public Issue changeStatus(Long issueId, IssueStatus targetStatus) {
        Issue issue = findById(issueId);
        List<IssueStatus> allowedStatuses = ALLOWED_STATUS_TRANSITIONS.get(issue.getStatus());

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
    @IsAdmin
    public Issue assign(Long issueId, Long assigneeId) {
        Issue issue = findById(issueId);
        User assignee = userService.findById(assigneeId);

        if (!assignee.isEnabled()) {
            throw new UserDisabledException();
        }

        issue.changeAssigneeTo(assignee);
        return issue;
    }

    @Transactional
    @IsAdmin
    public Issue unassign(Long issueId) {
        Issue issue = findById(issueId);

        issue.changeAssigneeTo(null);
        return issue;
    }

    @Transactional
    @IsAdmin
    public void delete(Long issueId) {
        Issue issue = findById(issueId);
        issueRepository.delete(issue);
    }

    private Pageable getValidatedPageable(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidSortFieldException(order.getProperty(), ALLOWED_SORT_FIELDS);
            }
        }

        // Add 'id' as a secondary sort field in case issues have the same primary sort field
        Sort stableSort = pageable.getSort().and(
            Sort.by(Sort.Order.asc("id"))
        );

        return PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            stableSort
        );
    }
}
