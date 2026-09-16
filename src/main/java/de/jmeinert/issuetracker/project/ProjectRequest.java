package de.jmeinert.issuetracker.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
    @NotBlank
    @Size(max = 150)
    String name,

    @Schema(types = {"string", "null"})
    @Size(max = 1000)
    String description
) {
}
