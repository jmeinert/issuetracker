package de.jmeinert.issuetracker.user;

public record ChangeUserEnabledResponse(boolean enabled) {

    public static ChangeUserEnabledResponse from(User user) {
        return new ChangeUserEnabledResponse(user.isEnabled());
    }
}
