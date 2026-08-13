package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectService;

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

    public IssueService(
        IssueRepository issueRepository,
        ProjectService projectService
    ) {
        this.issueRepository = issueRepository;
        this.projectService = projectService;
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
