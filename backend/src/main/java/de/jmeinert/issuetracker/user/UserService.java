package de.jmeinert.issuetracker.user;

import de.jmeinert.issuetracker.security.AuthenticatedUserProvider;
import de.jmeinert.issuetracker.security.IsAdmin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UserService(
        UserRepository userRepository,
        AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.userRepository = userRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User create(String email, String displayName, String passwordHash) {
        User user = new User(
            email,
            displayName,
            passwordHash,
            UserRole.USER,
            true
        );

        return userRepository.save(user);
    }

    @Transactional
    @IsAdmin
    public User changeEnabled(Long userId, boolean enabled) {
        if (!enabled && authenticatedUserProvider.getUserId().equals(userId)) {
            throw new SelfDeactivationNotAllowedException();
        }

        User user = findById(userId);

        user.changeEnabledTo(enabled);
        return user;
    }
}
