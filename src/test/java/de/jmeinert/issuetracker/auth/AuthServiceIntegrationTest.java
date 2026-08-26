package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.config.PersistenceConfig;
import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.user.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TestcontainersConfiguration.class,
    PersistenceConfig.class,
    AuthService.class,
    UserService.class
})
class AuthServiceIntegrationTest {

    @MockitoSpyBean
    private UserService userService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private TokenService tokenService;

    @Autowired
    private AuthService authService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Test
    void register_throwsUserAlreadyExistsException_whenUniqueConstraintIsViolated() {
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

        doReturn(false)
            .when(userService)
            .existsByUsernameOrEmail(normalizedUsername, normalizedEmail);

        when(passwordEncoder.encode(password))
            .thenReturn(encodedPassword);

        // 1st registration
        authService.register(request);

        // 2nd registration
        // Cause a `DataIntegrityViolationException` with stubbed existence check
        assertThrows(
            UserAlreadyExistsException.class,
            () -> authService.register(request)
        );
    }
}
