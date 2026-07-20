package de.jmeinert.issuetracker.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueRequest(
    @NotBlank
    @Size(max = 150)
    String title,

    @Size(max = 1000)
    String description,

    @NotNull
    IssueStatus status,

    @NotNull
    IssuePriority priority
) {
}
