package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.user.User;

public class IssueTestBuilder {

    private String title = "TestTitle";

    private String description = "TestDescription";

    private IssueStatus status = IssueStatus.OPEN;

    private IssuePriority priority = IssuePriority.LOW;

    private final Project project;

    private final User reporter;

    private User assignee;

    public IssueTestBuilder(Project project, User reporter) {
        this.project = project;
        this.reporter = reporter;
    }

    public IssueTestBuilder title(String title) {
        this.title = title;
        return this;
    }

    public IssueTestBuilder description(String description) {
        this.description = description;
        return this;
    }

    public IssueTestBuilder status(IssueStatus status) {
        this.status = status;
        return this;
    }

    public IssueTestBuilder priority(IssuePriority priority) {
        this.priority = priority;
        return this;
    }

    public IssueTestBuilder assignee(User assignee) {
        this.assignee = assignee;
        return this;
    }

    public Issue build() {
        return new Issue(
            title,
            description,
            status,
            priority,
            project,
            reporter,
            assignee
        );
    }
}
