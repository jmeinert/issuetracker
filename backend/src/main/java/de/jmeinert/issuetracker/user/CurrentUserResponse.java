package de.jmeinert.issuetracker.user;

public record CurrentUserResponse(
    Long id,
    String email,
    String displayName,
    UserRole role
) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getRole()
        );
    }
}
