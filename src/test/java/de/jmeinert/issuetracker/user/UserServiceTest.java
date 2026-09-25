package de.jmeinert.issuetracker.user;

import de.jmeinert.issuetracker.security.AuthenticatedUserProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    private UserService userService;

    @ParameterizedTest
    @CsvSource({
        "true, false",
        "false, true"
    })
    void changeEnabled_changesEnabledField_whenUserExists(
        boolean initialEnabled,
        boolean targetEnabled
    ) {
        Long userId = 1L;

        User user = new UserTestBuilder()
            .enabled(initialEnabled)
            .build();

        if (!targetEnabled) {
            when(authenticatedUserProvider.getUserId())
                .thenReturn(2L);
        }

        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

        User changedUser = userService.changeEnabled(userId, targetEnabled);

        assertEquals(targetEnabled, changedUser.isEnabled());
    }

    @Test
    void changeEnabled_throwsUserNotFoundException_whenUserDoesNotExist() {
        Long userId = 5L;

        when(authenticatedUserProvider.getUserId())
            .thenReturn(2L);

        when(userRepository.findById(userId))
            .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
            UserNotFoundException.class,
            () -> userService.changeEnabled(userId, false)
        );
        assertEquals("User not found with id: " + userId, exception.getMessage());
    }

    @Test
    void changeEnabled_throwsSelfDeactivationNotAllowedException_whenUserTriesToDisableTheirOwnAccount() {
        Long userId = 1L;

        when(authenticatedUserProvider.getUserId())
            .thenReturn(userId);

        SelfDeactivationNotAllowedException exception = assertThrows(
            SelfDeactivationNotAllowedException.class,
            () -> userService.changeEnabled(userId, false)
        );
        assertEquals("Disabling your own account is not allowed", exception.getMessage());

        verifyNoInteractions(userRepository);
    }
}
