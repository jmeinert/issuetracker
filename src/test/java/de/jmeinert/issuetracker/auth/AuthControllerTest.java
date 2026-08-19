package de.jmeinert.issuetracker.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void register_returns201_whenRequestIsValid() throws Exception {
        register("testuser", "test@example.com", "TestPassword1234")
            .andExpect(status().isCreated())
            .andExpect(content().string(""));

        verify(authService).register(
            new RegisterRequest("testuser", "test@example.com", "TestPassword1234")
        );
    }

    @Test
    void register_returns409_whenUserAlreadyExists() throws Exception {
        doThrow(new UserAlreadyExistsException())
            .when(authService)
            .register(any(RegisterRequest.class));

        register("testuser", "test@example.com", "TestPassword1234")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                .value("User with the supplied registration data already exists."));
    }

    @Test
    void register_returns400_whenUsernameIsBlank() throws Exception {
        register("", "test@example.com", "TestPassword1234")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.username").value("must not be blank"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_returns400_whenUsernameIsTooLong() throws Exception {
        String tooLongUsername = "a".repeat(51);

        register(tooLongUsername, "test@example.com", "TestPassword1234")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.username").value("size must be between 0 and 50"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_returns400_whenEmailIsBlank() throws Exception {
        register("testuser", "", "TestPassword1234")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.email").value("must not be blank"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_returns400_whenEmailIsTooLong() throws Exception {
        String tooLongEmail = "a".repeat(64) + "@"
            + "b".repeat(63) + "."
            + "c".repeat(63) + "."
            + "d".repeat(59) + ".com";

        register("testuser", tooLongEmail, "TestPassword1234")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.email").value("size must be between 0 and 255"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_returns400_whenEmailIsInvalid() throws Exception {
        register("testuser", "test.com", "TestPassword1234")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.email").value("must be a well-formed email address"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_returns400_whenPasswordIsBlank() throws Exception {
        String blankPassword = " ".repeat(15);

        register("testuser", "test@example.com", blankPassword)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.password").value("must not be blank"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_returns400_whenPasswordIsTooShort() throws Exception {
        String tooShortPassword = "a".repeat(14);

        register("testuser", "test@example.com", tooShortPassword)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.password").value("size must be between 15 and 128"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_returns400_whenPasswordIsTooLong() throws Exception {
        String tooLongPassword = "a".repeat(129);

        register("testuser", "test@example.com", tooLongPassword)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.password").value("size must be between 15 and 128"));

        verifyNoInteractions(authService);
    }

    private ResultActions register(
        String username,
        String email,
        String password
    ) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(username, email, password)));
    }
}
