package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.security.AuthenticatedUserProvider;

import org.springframework.stereotype.Component;

@Component
public class IssueAuthorization {

    private final IssueRepository issueRepository;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public IssueAuthorization(
        IssueRepository issueRepository,
        AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.issueRepository = issueRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public boolean canModify(Long issueId) {
        Long userId = authenticatedUserProvider.getUserId();
        return issueRepository.existsByIdAndParticipantId(issueId, userId);
    }
}
