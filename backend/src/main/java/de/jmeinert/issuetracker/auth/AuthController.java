package de.jmeinert.issuetracker.auth;

import de.jmeinert.issuetracker.openapi.BadRequestResponse;
import de.jmeinert.issuetracker.openapi.ConflictResponse;
import de.jmeinert.issuetracker.openapi.CreatedResponse;
import de.jmeinert.issuetracker.openapi.NoContentResponse;
import de.jmeinert.issuetracker.openapi.OkResponse;
import de.jmeinert.issuetracker.openapi.OpenApiConfig;
import de.jmeinert.issuetracker.openapi.UnauthorizedResponse;
import de.jmeinert.issuetracker.user.CurrentUserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a user")
    @CreatedResponse(description = "User created")
    @BadRequestResponse
    @ConflictResponse(description = "User already exists")
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

    @GetMapping("/api/auth/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Get CSRF token")
    @NoContentResponse
    public void csrf() {
    }

    @GetMapping("/api/auth/me")
    @Operation(summary = "Get currently logged-in user")
    @OkResponse(description = "Current user")
    @UnauthorizedResponse(description = "Not logged in")
    @SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH_SECURITY_SCHEME)
    public CurrentUserResponse me() {
        return CurrentUserResponse.from(authService.me());
    }
}
