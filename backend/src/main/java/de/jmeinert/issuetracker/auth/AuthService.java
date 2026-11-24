package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.security.AuthenticatedUserProvider;
import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserNormalizer;
import de.jmeinert.issuetracker.user.UserService;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AuthService(
        UserService userService,
        PasswordEncoder passwordEncoder,
        AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public void register(RegisterRequest request) {
        String normalizedEmail = UserNormalizer.normalizeEmail(request.email());

        if (userService.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(request.password());

        try {
            userService.create(normalizedEmail, request.displayName(), passwordHash);
        } catch (DataIntegrityViolationException exception) {
            // Handle race condition of parallel registrations
            if (isDuplicateUserConstraint(exception)) {
                throw new UserAlreadyExistsException();
            }

            throw exception;
        }
    }

    public User me() {
        return authenticatedUserProvider.findUserId()
            .map(userService::findById)
            .orElse(null);
    }

    private boolean isDuplicateUserConstraint(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (!(cause instanceof ConstraintViolationException constraintViolationException)) {
                continue;
            }

            String constraintName = constraintViolationException.getConstraintName();

            if (constraintName == null) {
                return false;
            }

            boolean hasDuplicateUserConstraintName = constraintName.equals("uq_users_email");
            boolean isUniqueConstraint
                = constraintViolationException.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE;
            return hasDuplicateUserConstraintName && isUniqueConstraint;
        }

        return false;
    }
}
