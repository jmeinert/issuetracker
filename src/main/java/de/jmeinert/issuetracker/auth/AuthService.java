package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.user.UserNormalizer;
import de.jmeinert.issuetracker.user.UserService;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private static final Set<String> DUPLICATE_USER_CONSTRAINT_NAMES = Set.of("uq_users_username", "uq_users_email");

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final TokenService tokenService;

    public AuthService(
        UserService userService,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        TokenService tokenService
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    public void register(RegisterRequest request) {
        String normalizedUsername = UserNormalizer.normalizeUsername(request.username());
        String normalizedEmail = UserNormalizer.normalizeEmail(request.email());

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

    public String login(LoginRequest request) {
        var authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
            request.username(),
            request.password()
        );

        Authentication authentication = authenticationManager.authenticate(authenticationRequest);

        return tokenService.generateToken(authentication);
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

            boolean hasDuplicateUserConstraintName
                = DUPLICATE_USER_CONSTRAINT_NAMES.contains(constraintName);
            boolean isUniqueConstraint
                = constraintViolationException.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE;
            return hasDuplicateUserConstraintName && isUniqueConstraint;
        }

        return false;
    }
}
