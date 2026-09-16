package de.jmeinert.issuetracker.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueRequest(
    @NotBlank
    @Size(max = 150)
    String title,

    @Schema(types = {"string", "null"})
    @Size(max = 1000)
    String description,

    @NotNull
    IssuePriority priority
) {
}
