package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.user.UserService;

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
    void register_normalizesUsernameAndEmailAndEncodesPassword() {
        String username = "   TestUser   ";
        String email = "Test@Example.com";
        String password = "TestPassword1234";
        String normalizedUsername = "testuser";
        String normalizedEmail = "test@example.com";
        String encodedPassword = "EncodedPassword";

        RegisterRequest request = new RegisterRequest(
            username,
            email,
            password
        );

        when(userService.existsByUsernameOrEmail(normalizedUsername, normalizedEmail))
            .thenReturn(false);

        when(passwordEncoder.encode(password))
            .thenReturn(encodedPassword);

        authService.register(request);

        verify(userService).existsByUsernameOrEmail(normalizedUsername, normalizedEmail);
        verify(passwordEncoder).encode(password);
        verify(userService).create(normalizedUsername, normalizedEmail, encodedPassword);
    }

    @Test
    void register_throwsUserAlreadyExistsException_whenUsernameOrEmailAlreadyExists() {
        String username = "   TestUser   ";
        String email = "Test@Example.com";
        String password = "TestPassword1234";
        String normalizedUsername = "testuser";
        String normalizedEmail = "test@example.com";

        RegisterRequest request = new RegisterRequest(
            username,
            email,
            password
        );

        when(userService.existsByUsernameOrEmail(normalizedUsername, normalizedEmail))
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
        verify(userService).existsByUsernameOrEmail(normalizedUsername, normalizedEmail);
        verify(userService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void register_rethrowsDataIntegrityViolationException_whenNotCausedByDuplicateUserConstraint() {
        String username = "   TestUser   ";
        String email = "Test@Example.com";
        String password = "TestPassword1234";
        String normalizedUsername = "testuser";
        String normalizedEmail = "test@example.com";
        String encodedPassword = "EncodedPassword";

        RegisterRequest request = new RegisterRequest(
            username,
            email,
            password
        );

        var expectedException = new DataIntegrityViolationException("Unrelated constraint violation");

        when(userService.existsByUsernameOrEmail(normalizedUsername, normalizedEmail))
            .thenReturn(false);

        when(passwordEncoder.encode(password))
            .thenReturn(encodedPassword);

        when(userService.create(normalizedUsername, normalizedEmail, encodedPassword))
            .thenThrow(expectedException);

        DataIntegrityViolationException actualException = assertThrows(
            DataIntegrityViolationException.class,
            () -> authService.register(request)
        );

        assertSame(expectedException, actualException);
    }
}
