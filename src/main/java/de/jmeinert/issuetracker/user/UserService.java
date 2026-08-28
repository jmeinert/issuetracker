package de.jmeinert.issuetracker.user;

import de.jmeinert.issuetracker.security.IsAdmin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean existsByUsernameOrEmail(String username, String email) {
        return userRepository.existsByUsernameOrEmail(username, email);
    }

    @Transactional
    public User create(String username, String email, String passwordHash) {
        User user = new User(
            username,
            email,
            passwordHash,
            UserRole.USER,
            true
        );

        return userRepository.save(user);
    }

    @Transactional
    @IsAdmin
    public User changeEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        user.changeEnabledTo(enabled);
        return user;
    }
}
