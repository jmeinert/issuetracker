package de.jmeinert.issuetracker.issue;

public class ClosedIssueUpdateException extends RuntimeException {

    public ClosedIssueUpdateException(Long id) {
        super("Issue with id " + id + " is closed and cannot be updated.");
    }
}
