package de.jmeinert.issuetracker.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank
    @Size(max = 50)
    String username,

    @NotBlank
    @Email
    @Size(max = 255)
    String email,

    @Schema(
        description = "Password for the new account",
        format = "password",
        accessMode = Schema.AccessMode.WRITE_ONLY
    )
    @NotBlank
    @Size(min = 15, max = 128)
    String password
) {
}
