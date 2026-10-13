package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserRepository;
import de.jmeinert.issuetracker.user.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestHelper(mockMvc);
    }

    @Test
    void register_persistsNormalizedUserWithEncodedPasswordAndDefaults() throws Exception {
        String username = "   TestUser   ";
        String email = "Test@Example.com";
        String password = "TestPassword1234";
        String normalizedUsername = "testuser";
        String normalizedEmail = "test@example.com";

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "username": "%s",
                        "email": "%s",
                        "password": "%s"
                    }
                    """.formatted(username, email, password)))
            .andExpect(status().isCreated());

        var users = userRepository.findAll();
        assertThat(users).hasSize(1);

        User user = users.getFirst();

        assertThat(user.getUsername()).isEqualTo(normalizedUsername);
        assertThat(user.getEmail()).isEqualTo(normalizedEmail);
        assertThat(user.getPasswordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, user.getPasswordHash())).isTrue();
        assertThat(user.getPasswordHash()).startsWith("{argon2id}");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void login_withUnnormalizedUsername_authenticatesUser(UserRole role) throws Exception {
        String username = "   TestUser   ";
        String normalizedUsername = "testuser";
        String email = "test@example.com";
        String password = "TestPassword1234";

        saveUser(normalizedUsername, email, password, role, true);

        MockHttpSession session = auth.login(username, password);

        mockMvc.perform(get("/api/projects")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(authenticated().withRoles(role.name()));
    }

    @Test
    void login_returns401_whenUsernameIsWrong() throws Exception {
        String username = "testuser";
        String email = "test@example.com";
        String password = "TestPassword1234";
        String wrongUsername = "testuser2";

        saveUser(username, email, password, UserRole.USER, true);

        mockMvc.perform(formLogin("/api/auth/login")
                .user(wrongUsername)
                .password(password))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_returns401_whenPasswordIsWrong() throws Exception {
        String username = "testuser";
        String email = "test@example.com";
        String password = "TestPassword1234";
        String wrongPassword = "TestPassword12345";

        saveUser(username, email, password, UserRole.USER, true);

        mockMvc.perform(formLogin("/api/auth/login")
                .user(username)
                .password(wrongPassword))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_returns401_whenUserIsDeactivated() throws Exception {
        String username = "testuser";
        String email = "test@example.com";
        String password = "TestPassword1234";

        saveUser(username, email, password, UserRole.USER, false);

        mockMvc.perform(formLogin("/api/auth/login")
                .user(username)
                .password(password))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void logout_invalidatesSession() throws Exception {
        String username = "testuser";
        String email = "test@example.com";
        String password = "TestPassword1234";

        saveUser(username, email, password, UserRole.USER, true);

        MockHttpSession session = auth.login(username, password);

        mockMvc.perform(get("/api/projects")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(authenticated());

        mockMvc.perform(post("/api/auth/logout")
                .with(csrf())
                .session(session))
            .andExpect(status().isNoContent())
            .andExpect(unauthenticated());

        assertThat(session.isInvalid()).isTrue();

        // Try protected endpoint with old session again
        mockMvc.perform(get("/api/projects")
                .session(session))
            .andExpect(status().isUnauthorized())
            .andExpect(unauthenticated());
    }

    @Test
    void logout_keepsSessionValid_whenCSRFTokenIsInvalid() throws Exception {
        String username = "testuser";
        String email = "test@example.com";
        String password = "TestPassword1234";

        saveUser(username, email, password, UserRole.USER, true);

        MockHttpSession session = auth.login(username, password);

        mockMvc.perform(post("/api/auth/logout")
                .with(csrf().useInvalidToken())
                .session(session))
            .andExpect(status().isForbidden())
            .andExpect(authenticated());

        assertThat(session.isInvalid()).isFalse();

        mockMvc.perform(get("/api/projects")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(authenticated());
    }

    private void saveUser(String username, String email, String password, UserRole role, boolean enabled) {
        userRepository.saveAndFlush(new User(
            username,
            email,
            passwordEncoder.encode(password),
            role,
            enabled
        ));
    }
}
