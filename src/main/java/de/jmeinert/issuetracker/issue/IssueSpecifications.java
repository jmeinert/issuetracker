package de.jmeinert.issuetracker.issue;

import org.springframework.data.jpa.domain.Specification;

public final class IssueSpecifications {

    private IssueSpecifications() {
    }

    public static Specification<Issue> hasProjectId(Long projectId) {
        return (root, query, criteriaBuilder)
            -> criteriaBuilder.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Issue> hasStatus(IssueStatus status) {
        return (root, query, criteriaBuilder)
            -> criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Issue> hasPriority(IssuePriority priority) {
        return (root, query, criteriaBuilder)
            -> criteriaBuilder.equal(root.get("priority"), priority);
    }
}
