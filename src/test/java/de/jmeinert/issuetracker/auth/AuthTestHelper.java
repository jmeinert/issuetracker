package de.jmeinert.issuetracker.auth;

import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthTestHelper {

    private final MockMvc mockMvc;

    public AuthTestHelper(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public MockHttpSession login(String username, String password) throws Exception {
        var result = mockMvc.perform(formLogin("/api/auth/login")
                .user(username)
                .password(password))
            .andExpect(status().isNoContent())
            .andExpect(authenticated())
            .andReturn();

        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }
}
