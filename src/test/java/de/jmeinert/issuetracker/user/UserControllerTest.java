package de.jmeinert.issuetracker.user;

import de.jmeinert.issuetracker.BaseSecurityWebMvcTest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@WithMockUser(roles = "ADMIN")
class UserControllerTest extends BaseSecurityWebMvcTest {

    @MockitoBean
    private UserService userService;

    @Test
    void changeUserEnabled_returns200_whenUserExists() throws Exception {
        Long userId = 1L;

        User user = new UserTestBuilder()
            .username("testuser")
            .email("testuser@example.com")
            .role(UserRole.USER)
            .enabled(false)
            .build();

        when(userService.changeEnabled(userId, false))
            .thenReturn(user);

        mockMvc.perform(patch("/api/users/{userId}/enabled", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "enabled": false
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("testuser@example.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void changeUserEnabled_returns404_whenUserDoesNotExist() throws Exception {
        Long userId = 5L;

        when(userService.changeEnabled(userId, false))
            .thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(patch("/api/users/{userId}/enabled", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "enabled": false
                }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found with id: " + userId));
    }

    @Test
    void changeUserEnabled_returns400_whenEnabledIsMissing() throws Exception {
        mockMvc.perform(patch("/api/users/1/enabled")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.enabled").value("must not be null"));

        verifyNoInteractions(userService);
    }
}
