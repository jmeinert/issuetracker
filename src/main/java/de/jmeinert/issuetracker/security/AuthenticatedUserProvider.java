package de.jmeinert.issuetracker.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    public Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Authenticated user ID is invalid", exception);
        }
    }
}
