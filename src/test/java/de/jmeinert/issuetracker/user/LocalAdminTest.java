package de.jmeinert.issuetracker.user;

import de.jmeinert.issuetracker.config.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles({"local", "test"})
@Import(TestcontainersConfiguration.class)
class LocalAdminTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void localAdminIsSeeded() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        assertThat(admin.getEmail()).isEqualTo("admin@localhost.invalid");
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getEnabled()).isTrue();
        assertThat(admin.getPasswordHash()).startsWith("{argon2id}");
        assertThat(passwordEncoder.matches(
            "testpassword1234",
            admin.getPasswordHash()
        )).isTrue();
    }
}
