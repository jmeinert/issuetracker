package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.user.UserRepository;
import de.jmeinert.issuetracker.user.UserTestBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthHttpIT {

    @Autowired
    private RestTestClient client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String username = "testuser";

    private final String password = "TestPassword1234";

    @BeforeEach
    void setUp() {
        userRepository.saveAndFlush(new UserTestBuilder()
            .username(username)
            .passwordHash(passwordEncoder.encode(password))
            .build());
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAllInBatch();
    }

    @Test
    void sessionCookie_allowsAccessUntilLogout() {
        ResponseCookie csrfCookie = client.get().uri("/api/auth/csrf")
            .exchange()
            .expectStatus().isNoContent()
            .returnResult().getResponseCookies().getFirst("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.isHttpOnly()).isFalse();
        String csrf = csrfCookie.getValue();

        var form = new LinkedMultiValueMap<String, String>();
        form.add("username", username);
        form.add("password", password);

        ResponseCookie sessionCookie = client.post().uri("/api/auth/login")
            .cookie("XSRF-TOKEN", csrf)
            .header("X-XSRF-TOKEN", csrf)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .exchange()
            .expectStatus().isNoContent()
            .returnResult().getResponseCookies().getFirst("JSESSIONID");
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.isHttpOnly()).isTrue();
        assertThat(sessionCookie.getSameSite()).isEqualTo("Lax");
        String session = sessionCookie.getValue();

        client.get().uri("/api/projects")
            .cookie("JSESSIONID", session)
            .exchange()
            .expectStatus().isOk();

        // Login clears the previous CSRF cookie
        csrfCookie = client.get().uri("/api/auth/csrf")
            .cookie("JSESSIONID", session)
            .exchange()
            .expectStatus().isNoContent()
            .returnResult().getResponseCookies().getFirst("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.isHttpOnly()).isFalse();
        csrf = csrfCookie.getValue();

        ResponseCookie sessionDeletionCookie = client.post().uri("/api/auth/logout")
            .cookie("JSESSIONID", session)
            .cookie("XSRF-TOKEN", csrf)
            .header("X-XSRF-TOKEN", csrf)
            .exchange()
            .expectStatus().isNoContent()
            .returnResult().getResponseCookies().getFirst("JSESSIONID");
        assertThat(sessionDeletionCookie).isNotNull();
        assertThat(sessionDeletionCookie.getMaxAge()).isZero();

        client.get().uri("/api/projects")
            .cookie("JSESSIONID", session)
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
