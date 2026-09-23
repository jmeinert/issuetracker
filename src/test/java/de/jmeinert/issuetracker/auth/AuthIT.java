package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserRepository;
import de.jmeinert.issuetracker.user.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
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

    @Autowired
    private JsonMapper jsonMapper;

    private AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestHelper(mockMvc, jsonMapper);
    }

    @Test
    void register_persistsNormalizedUserWithEncodedPasswordAndDefaults() throws Exception {
        String username = "   TestUser   ";
        String email = "Test@Example.com";
        String password = "TestPassword1234";
        String normalizedUsername = "testuser";
        String normalizedEmail = "test@example.com";

        mockMvc.perform(post("/api/auth/register")
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
        assertThat(user.getEnabled()).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void registerAndLogin_authenticatesUserAndReturnsUsableJWT() throws Exception {
        String username = "testuser";
        String email = "test@example.com";
        String password = "TestPassword1234";

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(username, email, password)))
            .andExpect(status().isCreated());

        String token = auth.login(username, password);

        mockMvc.perform(get("/api/projects")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(authenticated().withRoles("USER"));
    }

    @Test
    void login_withUnnormalizedUsername_mapsAdminRoleClaimToValidRole() throws Exception {
        String username = "   TestUser   ";
        String normalizedUsername = "testuser";
        String email = "test@example.com";
        String password = "TestPassword1234";

        saveUser(normalizedUsername, email, password, UserRole.ADMIN, true);

        String token = auth.login(username, password);

        mockMvc.perform(get("/api/projects")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void login_returns401_whenUsernameIsWrong() throws Exception {
        String username = "testuser";
        String email = "test@example.com";
        String password = "TestPassword1234";
        String wrongUsername = "testuser2";

        saveUser(username, email, password, UserRole.USER, true);

        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(wrongUsername, password)))
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

        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, wrongPassword)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_returns401_whenUserIsDeactivated() throws Exception {
        String username = "testuser";
        String email = "test@example.com";
        String password = "TestPassword1234";

        saveUser(username, email, password, UserRole.USER, false);

        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
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
