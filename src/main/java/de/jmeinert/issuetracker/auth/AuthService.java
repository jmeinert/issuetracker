package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.user.UserService;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private static final Set<String> DUPLICATE_USER_CONSTRAINT_NAMES = Set.of("uq_users_username", "uq_users_email");

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    public AuthService(
        UserService userService,
        PasswordEncoder passwordEncoder
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequest request) {
        String normalizedUsername = request.username().strip().toLowerCase(Locale.ROOT);
        String normalizedEmail = request.email().strip().toLowerCase(Locale.ROOT);

        if (userService.existsByUsernameOrEmail(normalizedUsername, normalizedEmail)) {
            throw new UserAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(request.password());

        try {
            userService.create(normalizedUsername, normalizedEmail, passwordHash);
        } catch (DataIntegrityViolationException exception) {
            // Handle race condition of parallel registrations
            if (isDuplicateUserConstraint(exception)) {
                throw new UserAlreadyExistsException();
            }

            throw exception;
        }
    }

    private boolean isDuplicateUserConstraint(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (!(cause instanceof ConstraintViolationException constraintViolationException)) {
                continue;
            }

            boolean hasDuplicateUserConstraintName
                = DUPLICATE_USER_CONSTRAINT_NAMES.contains(constraintViolationException.getConstraintName());
            boolean isUniqueConstraint
                = constraintViolationException.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE;
            return hasDuplicateUserConstraintName && isUniqueConstraint;
        }

        return false;
    }
}
