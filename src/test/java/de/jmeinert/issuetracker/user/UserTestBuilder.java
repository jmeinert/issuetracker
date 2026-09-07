package de.jmeinert.issuetracker.user;

import org.springframework.test.util.ReflectionTestUtils;

public class UserTestBuilder {

    private Long id;

    private String username = "testuser";

    private String email = "testuser@example.com";

    private String passwordHash = "passwordHash";

    private UserRole role = UserRole.USER;

    private boolean enabled = true;

    public UserTestBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public UserTestBuilder username(String username) {
        this.username = username;
        return this;
    }

    public UserTestBuilder email(String email) {
        this.email = email;
        return this;
    }

    public UserTestBuilder passwordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public UserTestBuilder role(UserRole role) {
        this.role = role;
        return this;
    }

    public UserTestBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public User build() {
        User user = new User(
            username,
            email,
            passwordHash,
            role,
            enabled
        );

        if (id != null) {
            ReflectionTestUtils.setField(user, "id", id);
        }

        return user;
    }
}
