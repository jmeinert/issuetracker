package de.jmeinert.issuetracker.project;

import java.time.Instant;

public record ProjectResponse(
    Long id,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
