package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.user.UserService;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_normalizesEmailAndEncodesPassword() {
        String email = "Test@Example.com";
        String displayName = "Test User";
        String password = "TestPassword1234";
        String normalizedEmail = "test@example.com";
        String encodedPassword = "EncodedPassword";

        RegisterRequest request = new RegisterRequest(
            email,
            displayName,
            password
        );

        when(userService.existsByEmail(normalizedEmail))
            .thenReturn(false);

        when(passwordEncoder.encode(password))
            .thenReturn(encodedPassword);

        authService.register(request);

        verify(userService).existsByEmail(normalizedEmail);
        verify(passwordEncoder).encode(password);
        verify(userService).create(normalizedEmail, displayName, encodedPassword);
    }

    @Test
    void register_throwsUserAlreadyExistsException_whenUsernameOrEmailAlreadyExists() {
        String email = "test@example.com";
        String displayName = "Test User";
        String password = "TestPassword1234";

        RegisterRequest request = new RegisterRequest(
            email,
            displayName,
            password
        );

        when(userService.existsByEmail(email))
            .thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
            UserAlreadyExistsException.class,
            () -> authService.register(request)
        );
        assertEquals(
            "User with the supplied registration data already exists.",
            exception.getMessage()
        );

        verifyNoInteractions(passwordEncoder);
        verify(userService).existsByEmail(email);
        verify(userService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void register_rethrowsDataIntegrityViolationException_whenNotCausedByDuplicateUserConstraint() {
        String email = "test@example.com";
        String displayName = "Test User";
        String password = "TestPassword1234";
        String encodedPassword = "EncodedPassword";

        RegisterRequest request = new RegisterRequest(
            email,
            displayName,
            password
        );

        var expectedException = new DataIntegrityViolationException("Unrelated constraint violation");

        when(userService.existsByEmail(email))
            .thenReturn(false);

        when(passwordEncoder.encode(password))
            .thenReturn(encodedPassword);

        when(userService.create(email, displayName, encodedPassword))
            .thenThrow(expectedException);

        DataIntegrityViolationException actualException = assertThrows(
            DataIntegrityViolationException.class,
            () -> authService.register(request)
        );

        assertSame(expectedException, actualException);
    }

    @Test
    void register_rethrowsDataIntegrityViolationException_whenConstraintNameIsUnknown() {
        String email = "test@example.com";
        String displayName = "Test User";
        String password = "TestPassword1234";
        String encodedPassword = "EncodedPassword";

        RegisterRequest request = new RegisterRequest(
            email,
            displayName,
            password
        );

        var cause = new ConstraintViolationException("Violation with unknown constraint name", null, null);
        var expectedException = new DataIntegrityViolationException("Some violation", cause);

        when(userService.existsByEmail(email))
            .thenReturn(false);

        when(passwordEncoder.encode(password))
            .thenReturn(encodedPassword);

        when(userService.create(email, displayName, encodedPassword))
            .thenThrow(expectedException);

        DataIntegrityViolationException actualException = assertThrows(
            DataIntegrityViolationException.class,
            () -> authService.register(request)
        );

        assertSame(expectedException, actualException);
    }
}
