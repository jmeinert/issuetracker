package de.jmeinert.issuetracker.issue;

import java.time.Instant;

public record IssueResponse(
    Long id,
    String title,
    String description,
    IssueStatus status,
    IssuePriority priority,
    Long projectId,
    Instant createdAt,
    Instant updatedAt
) {

    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
            issue.getId(),
            issue.getTitle(),
            issue.getDescription(),
            issue.getStatus(),
            issue.getPriority(),
            issue.getProject().getId(),
            issue.getCreatedAt(),
            issue.getUpdatedAt()
        );
    }
}
