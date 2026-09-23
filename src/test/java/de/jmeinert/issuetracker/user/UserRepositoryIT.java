package de.jmeinert.issuetracker.user;

import de.jmeinert.issuetracker.config.PersistenceConfig;
import de.jmeinert.issuetracker.config.TestcontainersConfiguration;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, PersistenceConfig.class})
class UserRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void existsByUsernameOrEmail_returnsTrue_whenUserHasUsername() {
        String username = "testuser";

        User user = new UserTestBuilder()
            .username(username)
            .build();

        userRepository.saveAndFlush(user);
        entityManager.clear();

        assertThat(userRepository.existsByUsernameOrEmail(username, "test2@test.com"))
            .isTrue();
    }

    @Test
    void existsByUsernameOrEmail_returnsTrue_whenUserHasEmail() {
        String email = "testuser@example.com";

        User user = new UserTestBuilder()
            .email(email)
            .build();

        userRepository.saveAndFlush(user);
        entityManager.clear();

        assertThat(userRepository.existsByUsernameOrEmail("testuser2", email))
            .isTrue();
    }

    @Test
    void existsByUsernameOrEmail_returnsFalse_whenNoUserHasUsernameOrEmail() {
        User user = new UserTestBuilder()
            .username("testuser")
            .email("testuser@example.com")
            .build();

        userRepository.saveAndFlush(user);
        entityManager.clear();

        assertThat(userRepository.existsByUsernameOrEmail("testuser2", "testuser2@example.com"))
            .isFalse();
    }

    @Test
    void save_persistsUser() {
        User user = new UserTestBuilder()
            .username("testuser")
            .email("testuser@example.com")
            .passwordHash("passwordHash")
            .role(UserRole.USER)
            .enabled(false)
            .build();
        userRepository.saveAndFlush(user);

        Long userId = user.getId();

        entityManager.clear();

        User persistedUser = userRepository.findById(user.getId())
            .orElseThrow();

        assertThat(persistedUser.getId()).isEqualTo(userId);
        assertThat(persistedUser.getUsername()).isEqualTo("testuser");
        assertThat(persistedUser.getEmail()).isEqualTo("testuser@example.com");
        assertThat(persistedUser.getPasswordHash()).isEqualTo("passwordHash");
        assertThat(persistedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(persistedUser.isEnabled()).isFalse();
        assertThat(persistedUser.getCreatedAt()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void save_acceptsEveryUserRole(UserRole role) {
        User user = new UserTestBuilder()
            .role(role)
            .build();

        userRepository.saveAndFlush(user);
        entityManager.clear();

        User persistedUser = userRepository.findById(user.getId())
            .orElseThrow();

        assertThat(persistedUser.getRole())
            .isEqualTo(role);
    }

    @Test
    void save_acceptsMaximumFieldLengths() {
        String username = "a".repeat(50);
        String email = "a".repeat(255);
        String passwordHash = "a".repeat(255);

        User user = new UserTestBuilder()
            .username(username)
            .email(email)
            .passwordHash(passwordHash)
            .build();

        userRepository.saveAndFlush(user);
        entityManager.clear();

        User persistedUser = userRepository.findById(user.getId())
            .orElseThrow();

        assertThat(persistedUser.getUsername())
            .isEqualTo(username);
        assertThat(persistedUser.getEmail())
            .isEqualTo(email);
        assertThat(persistedUser.getPasswordHash())
            .isEqualTo(passwordHash);
    }

    @Test
    void save_rejectsDuplicateUsername() {
        User user1 = new UserTestBuilder()
            .username("testuser")
            .email("testuser@example.com")
            .build();
        userRepository.saveAndFlush(user1);
        entityManager.clear();

        User user2 = new UserTestBuilder()
            .username("testuser")
            .email("testuser2@example.com")
            .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
            .isInstanceOf(DataIntegrityViolationException.class)
            .rootCause()
            .isInstanceOfSatisfying(PSQLException.class, exception -> {
                assertThat(exception.getSQLState())
                    .isEqualTo(PSQLState.UNIQUE_VIOLATION.getState());
                assertThat(exception.getServerErrorMessage())
                    .isNotNull();
                assertThat(exception.getServerErrorMessage().getConstraint())
                    .isEqualTo("uq_users_username");
            });
    }

    @Test
    void save_rejectsDuplicateEmail() {
        User user1 = new UserTestBuilder()
            .username("testuser")
            .email("testuser@example.com")
            .build();
        userRepository.saveAndFlush(user1);
        entityManager.clear();

        User user2 = new UserTestBuilder()
            .username("testuser2")
            .email("testuser@example.com")
            .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
            .isInstanceOf(DataIntegrityViolationException.class)
            .rootCause()
            .isInstanceOfSatisfying(PSQLException.class, exception -> {
                assertThat(exception.getSQLState())
                    .isEqualTo(PSQLState.UNIQUE_VIOLATION.getState());
                assertThat(exception.getServerErrorMessage())
                    .isNotNull();
                assertThat(exception.getServerErrorMessage().getConstraint())
                    .isEqualTo("uq_users_email");
            });
    }
}
