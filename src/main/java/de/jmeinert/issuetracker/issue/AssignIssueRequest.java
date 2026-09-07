package de.jmeinert.issuetracker.issue;

import jakarta.validation.constraints.NotNull;

public record AssignIssueRequest(@NotNull Long assigneeId) {
}
