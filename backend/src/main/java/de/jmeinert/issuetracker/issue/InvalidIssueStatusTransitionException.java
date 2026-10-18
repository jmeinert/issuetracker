package de.jmeinert.issuetracker.issue;

import java.util.List;

public class InvalidIssueStatusTransitionException extends RuntimeException {

    public InvalidIssueStatusTransitionException(
        Long issueId,
        IssueStatus currentStatus,
        IssueStatus targetStatus,
        List<IssueStatus> allowedStatuses
    ) {
        super(
            "Issue with id %d cannot transition from '%s' to '%s'. Allowed target statuses: %s."
                .formatted(
                    issueId,
                    currentStatus,
                    targetStatus,
                    String.join(
                        ", ",
                        allowedStatuses.stream().map(IssueStatus::name).toList()
                    )
                )
        );
    }
}
