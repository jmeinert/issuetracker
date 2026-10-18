package de.jmeinert.issuetracker.issue;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.EscapeCharacter;

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

    public static Specification<Issue> hasSearchText(String searchText) {
        String escapedSearchText = EscapeCharacter.DEFAULT.escape(searchText);

        return (root, query, criteriaBuilder) -> {
            Predicate issueTitlePredicate = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("title")),
                "%" + escapedSearchText + "%",
                EscapeCharacter.DEFAULT.getEscapeCharacter()
            );
            Predicate issueDescriptionPredicate = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("description")),
                "%" + escapedSearchText + "%",
                EscapeCharacter.DEFAULT.getEscapeCharacter()
            );

            return criteriaBuilder.or(issueTitlePredicate, issueDescriptionPredicate);
        };
    }
}
