package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.config.PersistenceConfig;
import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.user.UserRepository;
import de.jmeinert.issuetracker.user.UserService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class AuthServiceIT {

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

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAllInBatch();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @ParameterizedTest
    @CsvSource({
        "testuser, test2@example.com",
        "testuser2, test@example.com"
    })
    void register_throwsUserAlreadyExistsException_whenUniqueConstraintIsViolated(
        String secondUsername,
        String secondEmail
    ) {
        String password = "TestPassword1234";
        String encodedPassword = "EncodedPassword";

        RegisterRequest firstRequest = new RegisterRequest(
            "testuser",
            "test@example.com",
            password
        );

        RegisterRequest secondRequest = new RegisterRequest(
            secondUsername,
            secondEmail,
            password
        );

        doReturn(false)
            .when(userService)
            .existsByUsernameOrEmail("testuser", "test@example.com");

        doReturn(false)
            .when(userService)
            .existsByUsernameOrEmail(secondUsername, secondEmail);

        when(passwordEncoder.encode(password))
            .thenReturn(encodedPassword);

        // 1st registration
        authService.register(firstRequest);

        // 2nd registration
        // Cause a `DataIntegrityViolationException` with stubbed existence check
        assertThrows(
            UserAlreadyExistsException.class,
            () -> authService.register(secondRequest)
        );
    }
}
