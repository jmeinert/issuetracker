package de.jmeinert.issuetracker.security;

import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserNormalizer;
import de.jmeinert.issuetracker.user.UserService;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@NullMarked
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public DatabaseUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = UserNormalizer.normalizeEmail(email);
        User user = userService.findByEmail(normalizedEmail)
            .orElseThrow(() -> new UsernameNotFoundException(normalizedEmail));

        return new UserPrincipal(user);
    }
}
