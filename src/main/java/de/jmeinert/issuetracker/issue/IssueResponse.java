package de.jmeinert.issuetracker.issue;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record IssueResponse(
    Long id,
    String title,
    @Schema(types = {"string", "null"}) String description,
    IssueStatus status,
    IssuePriority priority,
    Long projectId,
    Long reporterId,
    @Schema(types = {"integer", "null"}) Long assigneeId,
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
            issue.getReporter().getId(),
            issue.getAssignee() != null ? issue.getAssignee().getId() : null,
            issue.getCreatedAt(),
            issue.getUpdatedAt()
        );
    }
}
