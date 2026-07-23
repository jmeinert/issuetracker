package de.jmeinert.issuetracker.issue;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping("/api/issues/{issueId}")
    public IssueResponse getIssueById(@PathVariable Long issueId) {
        return IssueResponse.from(issueService.findById(issueId));
    }

    @GetMapping("/api/projects/{projectId}/issues")
    public List<IssueResponse> getIssuesByProjectId(@PathVariable Long projectId) {
        return issueService.findAllByProjectId(projectId).stream()
            .map(IssueResponse::from)
            .toList();
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

    @DeleteMapping("/api/issues/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(@PathVariable Long issueId) {
        issueService.delete(issueId);
    }
}
