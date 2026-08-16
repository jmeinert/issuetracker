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
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void save_persistsUser() {
        User user = new User(
            "TestUser",
            "test@test.com",
            "TestPasswordHash",
            UserRole.USER,
            false
        );
        userRepository.saveAndFlush(user);

        Long userId = user.getId();

        entityManager.clear();

        User persistedUser = userRepository.findById(user.getId())
            .orElseThrow();

        assertThat(persistedUser.getId()).isEqualTo(userId);
        assertThat(persistedUser.getUsername()).isEqualTo("TestUser");
        assertThat(persistedUser.getEmail()).isEqualTo("test@test.com");
        assertThat(persistedUser.getPasswordHash()).isEqualTo("TestPasswordHash");
        assertThat(persistedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(persistedUser.getEnabled()).isFalse();
        assertThat(persistedUser.getCreatedAt()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void save_acceptsEveryUserRole(UserRole role) {
        User user = new User(
            "TestUser",
            "test@test.com",
            "TestPasswordHash",
            role,
            true
        );

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

        User user = new User(
            username,
            email,
            passwordHash,
            UserRole.USER,
            true
        );

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
        User user1 = new User(
            "TestUser",
            "test@test.com",
            "TestPasswordHash",
            UserRole.USER,
            false
        );
        userRepository.saveAndFlush(user1);
        entityManager.clear();

        User user2 = new User(
            "TestUser",
            "test2@test.com",
            "TestPasswordHash2",
            UserRole.USER,
            false
        );

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
        User user1 = new User(
            "TestUser",
            "test@test.com",
            "TestPasswordHash",
            UserRole.USER,
            false
        );
        userRepository.saveAndFlush(user1);
        entityManager.clear();

        User user2 = new User(
            "TestUser2",
            "test@test.com",
            "TestPasswordHash2",
            UserRole.USER,
            false
        );

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
