package de.jmeinert.issuetracker.issue;

public record IssueFilter(
    Long projectId,
    IssueStatus status,
    IssuePriority priority,
    String search
) {

    public static IssueFilter empty() {
        return new IssueFilter(null, null, null, null);
    }
}
