package de.jmeinert.issuetracker.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
    @NotBlank String name,
    String description
) {
}
