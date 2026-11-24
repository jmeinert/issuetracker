package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.user.CurrentUserResponse;

import org.jspecify.annotations.Nullable;

public record CurrentSessionResponse(@Nullable CurrentUserResponse user) {
}
