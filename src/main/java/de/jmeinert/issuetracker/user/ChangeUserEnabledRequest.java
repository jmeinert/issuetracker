package de.jmeinert.issuetracker.user;

import jakarta.validation.constraints.NotNull;

public record ChangeUserEnabledRequest(
    @NotNull Boolean enabled
) {
}
