package de.jmeinert.issuetracker.issue;

import jakarta.validation.constraints.NotNull;

public record ChangeIssueStatusRequest(
    @NotNull IssueStatus status
) {
}
