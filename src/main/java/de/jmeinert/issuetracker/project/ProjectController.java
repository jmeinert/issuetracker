package de.jmeinert.issuetracker.project;

import de.jmeinert.issuetracker.openapi.BadRequestResponse;
import de.jmeinert.issuetracker.openapi.ConflictResponse;
import de.jmeinert.issuetracker.openapi.CreatedResponse;
import de.jmeinert.issuetracker.openapi.NoContentResponse;
import de.jmeinert.issuetracker.openapi.NotFoundResponse;
import de.jmeinert.issuetracker.openapi.OkResponse;
import de.jmeinert.issuetracker.openapi.OpenApiConfig;
import de.jmeinert.issuetracker.openapi.RestrictedEndpointResponses;
import de.jmeinert.issuetracker.openapi.UnauthorizedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@SecurityRequirement(name = OpenApiConfig.JWT_BEARER_SECURITY_SCHEME)
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "Retrieve all projects")
    @OkResponse(description = "List of projects")
    @UnauthorizedResponse
    public List<ProjectResponse> getAll() {
        return projectService.findAll().stream()
            .map(ProjectResponse::from)
            .toList();
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Retrieve a project by ID")
    @OkResponse(description = "Project")
    @NotFoundResponse(description = "Project not found")
    @UnauthorizedResponse
    public ProjectResponse getProjectById(@PathVariable Long projectId) {
        return ProjectResponse.from(projectService.findById(projectId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a project")
    @CreatedResponse(description = "Project created")
    @BadRequestResponse
    @RestrictedEndpointResponses
    public ProjectResponse createProject(@Valid @RequestBody ProjectRequest projectRequest) {
        return ProjectResponse.from(
            projectService.create(projectRequest.name(), projectRequest.description())
        );
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update a project")
    @OkResponse(description = "Project updated")
    @NotFoundResponse(description = "Project not found")
    @BadRequestResponse
    @RestrictedEndpointResponses
    public ProjectResponse updateProject(@PathVariable Long projectId, @Valid @RequestBody ProjectRequest projectRequest) {
        return ProjectResponse.from(
            projectService.update(projectId, projectRequest.name(), projectRequest.description())
        );
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a project")
    @NoContentResponse
    @NotFoundResponse(description = "Project not found")
    @ConflictResponse(description = "Project has issues")
    @RestrictedEndpointResponses
    public void deleteProject(@PathVariable Long projectId) {
        projectService.delete(projectId);
    }
}
