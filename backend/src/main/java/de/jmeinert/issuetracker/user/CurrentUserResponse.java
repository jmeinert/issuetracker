package de.jmeinert.issuetracker.user;

public record CurrentUserResponse(
    Long id,
    String username,
    UserRole role
) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
            user.getId(),
            user.getUsername(),
            user.getRole()
        );
    }
}
