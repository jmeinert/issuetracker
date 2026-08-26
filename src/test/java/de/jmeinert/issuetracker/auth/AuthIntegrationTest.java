package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserRepository;
import de.jmeinert.issuetracker.user.UserRole;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

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
    void login_authenticatesUserAndReturnsUsableJWT() throws Exception {
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

        String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        LoginResponse loginResponse = jsonMapper.readValue(response, LoginResponse.class);
        String token = loginResponse.token();

        // Access protected endpoint with given JWT token
        mockMvc.perform(get("/api/projects")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void login_returns401_whenUsernameIsWrong() throws Exception {
        String username = "testuser";
        String email = "test@example.com";
        String password = "TestPassword1234";
        String wrongUsername = "testuser2";

        saveUser(username, email, password, true);

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

        saveUser(username, email, password, true);

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

        saveUser(username, email, password, false);

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

    private void saveUser(String username, String email, String password, boolean enabled) {
        userRepository.saveAndFlush(new User(
            username,
            email,
            passwordEncoder.encode(password),
            UserRole.USER,
            enabled
        ));
    }
}
