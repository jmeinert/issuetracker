package de.jmeinert.issuetracker.user;

public record UserResponse(
    String username,
    String email,
    UserRole role,
    boolean enabled
) {

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.isEnabled()
        );
    }
}
