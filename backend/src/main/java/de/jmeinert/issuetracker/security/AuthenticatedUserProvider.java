package de.jmeinert.issuetracker.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticatedUserProvider {

    public Long getUserId() {
        return findUserId()
            .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    public Optional<Long> findUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        if (!(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new IllegalStateException("Principal is not of type UserPrincipal");
        }

        return Optional.of(userPrincipal.getId());
    }
}
