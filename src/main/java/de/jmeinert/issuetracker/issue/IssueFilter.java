package de.jmeinert.issuetracker.issue;

public record IssueFilter(
    Long projectId,
    IssueStatus status,
    IssuePriority priority
) {

    public static IssueFilter empty() {
        return new IssueFilter(null, null, null);
    }
}
