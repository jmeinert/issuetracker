package de.jmeinert.issuetracker.project;

public class ProjectHasIssuesException extends RuntimeException {

    public ProjectHasIssuesException(Long id) {
        super("Project with id " + id + " cannot be deleted because it still contains issues.");
    }
}
