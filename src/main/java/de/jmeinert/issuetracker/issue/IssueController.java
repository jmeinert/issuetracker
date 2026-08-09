package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.pagination.PageResponse;

import jakarta.validation.Valid;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping("/api/issues")
    public PageResponse<IssueResponse> getIssues(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        return PageResponse.from(
            issueService.findAll(pageable).map(IssueResponse::from)
        );
    }

    @GetMapping("/api/issues/{issueId}")
    public IssueResponse getIssueById(@PathVariable Long issueId) {
        return IssueResponse.from(issueService.findById(issueId));
    }

    @GetMapping("/api/projects/{projectId}/issues")
    public PageResponse<IssueResponse> getIssuesByProjectId(
        @PathVariable Long projectId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        return PageResponse.from(
            issueService.findAllByProjectId(projectId, pageable).map(IssueResponse::from)
        );
    }

    @PostMapping("/api/projects/{projectId}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse createIssue(@Valid @RequestBody CreateIssueRequest request, @PathVariable Long projectId) {
        return IssueResponse.from(issueService.create(projectId, request));
    }

    @PutMapping("/api/issues/{issueId}")
    public IssueResponse updateIssue(@Valid @RequestBody UpdateIssueRequest request, @PathVariable Long issueId) {
        return IssueResponse.from(issueService.update(issueId, request));
    }

    @PatchMapping("/api/issues/{issueId}/status")
    public IssueResponse changeIssueStatus(
        @Valid @RequestBody ChangeIssueStatusRequest request,
        @PathVariable Long issueId
    ) {
        return IssueResponse.from(issueService.changeStatus(issueId, request));
    }

    @DeleteMapping("/api/issues/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(@PathVariable Long issueId) {
        issueService.delete(issueId);
    }
}
