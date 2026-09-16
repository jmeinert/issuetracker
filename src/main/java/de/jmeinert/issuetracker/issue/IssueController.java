package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.openapi.BadRequestResponse;
import de.jmeinert.issuetracker.openapi.ConflictResponse;
import de.jmeinert.issuetracker.openapi.CreatedResponse;
import de.jmeinert.issuetracker.openapi.NoContentResponse;
import de.jmeinert.issuetracker.openapi.NotFoundResponse;
import de.jmeinert.issuetracker.openapi.OkResponse;
import de.jmeinert.issuetracker.openapi.OpenApiConfig;
import de.jmeinert.issuetracker.openapi.RestrictedEndpointResponses;
import de.jmeinert.issuetracker.openapi.UnauthorizedResponse;
import de.jmeinert.issuetracker.pagination.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = OpenApiConfig.JWT_BEARER_SECURITY_SCHEME)
@Tag(name = "Issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping("/api/issues")
    @IssuePaginationDocumentation(summary = "Query issues with pagination, sorting, filtering and text search")
    @OkResponse(description = "Page of issues")
    @BadRequestResponse
    @UnauthorizedResponse
    public PageResponse<IssueResponse> getIssues(
        @ParameterObject
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable,

        @Parameter(description = "Filter issues by project ID")
        @RequestParam(required = false)
        Long projectId,

        @Parameter(description = "Filter issues by status")
        @RequestParam(required = false)
        IssueStatus status,

        @Parameter(description = "Filter issues by priority")
        @RequestParam(required = false)
        IssuePriority priority,

        @Parameter(description = "Case-insensitive search in issue titles and descriptions")
        @RequestParam(required = false)
        @Size(max = 100)
        String search
    ) {
        IssueFilter filter = new IssueFilter(projectId, status, priority, search);
        return PageResponse.from(
            issueService.findAll(filter, pageable).map(IssueResponse::from)
        );
    }

    @GetMapping("/api/issues/{issueId}")
    @Operation(summary = "Retrieve an issue by ID")
    @OkResponse(description = "Issue")
    @NotFoundResponse(description = "Issue not found")
    @UnauthorizedResponse
    public IssueResponse getIssueById(@PathVariable Long issueId) {
        return IssueResponse.from(issueService.findById(issueId));
    }

    @GetMapping("/api/projects/{projectId}/issues")
    @IssuePaginationDocumentation(summary = "Retrieve paginated issues for a project")
    @OkResponse(description = "Page of issues")
    @NotFoundResponse(description = "Project not found")
    @BadRequestResponse
    @UnauthorizedResponse
    public PageResponse<IssueResponse> getIssuesByProjectId(
        @PathVariable Long projectId,

        @ParameterObject
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        return PageResponse.from(
            issueService.findAllByProjectId(projectId, pageable).map(IssueResponse::from)
        );
    }

    @PostMapping("/api/projects/{projectId}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an issue within a project")
    @CreatedResponse(description = "Issue created")
    @NotFoundResponse(description = "Project not found")
    @BadRequestResponse
    @UnauthorizedResponse
    public IssueResponse createIssue(@Valid @RequestBody CreateIssueRequest request, @PathVariable Long projectId) {
        return IssueResponse.from(issueService.create(projectId, request));
    }

    @PutMapping("/api/issues/{issueId}")
    @Operation(summary = "Update an issue")
    @OkResponse(description = "Issue updated")
    @NotFoundResponse(description = "Issue not found")
    @BadRequestResponse
    @ConflictResponse(description = "Issue is closed")
    @RestrictedEndpointResponses
    public IssueResponse updateIssue(@Valid @RequestBody UpdateIssueRequest request, @PathVariable Long issueId) {
        return IssueResponse.from(issueService.update(issueId, request));
    }

    @PatchMapping("/api/issues/{issueId}/status")
    @Operation(summary = "Change the status of an issue")
    @OkResponse(description = "Issue status changed")
    @NotFoundResponse(description = "Issue not found")
    @BadRequestResponse
    @ConflictResponse(description = "Invalid issue status transition")
    @RestrictedEndpointResponses
    public IssueResponse changeIssueStatus(
        @Valid @RequestBody ChangeIssueStatusRequest request,
        @PathVariable Long issueId
    ) {
        return IssueResponse.from(issueService.changeStatus(issueId, request));
    }

    @PatchMapping("/api/issues/{issueId}/assignee")
    @Operation(summary = "Assign or reassign an issue")
    @OkResponse(description = "Issue assigned to user")
    @NotFoundResponse(description = "Issue or assignee not found")
    @BadRequestResponse
    @ConflictResponse(description = "User is disabled")
    @RestrictedEndpointResponses
    public IssueResponse assignIssue(
        @Valid @RequestBody AssignIssueRequest request,
        @PathVariable Long issueId
    ) {
        return IssueResponse.from(issueService.assign(issueId, request));
    }

    @DeleteMapping("/api/issues/{issueId}/assignee")
    @Operation(summary = "Remove the current assignee")
    @OkResponse(description = "Issue without an assignee")
    @NotFoundResponse(description = "Issue not found")
    @RestrictedEndpointResponses
    public IssueResponse unassignIssue(@PathVariable Long issueId) {
        return IssueResponse.from(issueService.unassign(issueId));
    }

    @DeleteMapping("/api/issues/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an issue")
    @NoContentResponse
    @NotFoundResponse(description = "Issue not found")
    @RestrictedEndpointResponses
    public void deleteIssue(@PathVariable Long issueId) {
        issueService.delete(issueId);
    }
}
