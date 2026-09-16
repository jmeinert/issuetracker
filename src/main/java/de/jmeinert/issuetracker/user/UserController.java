package de.jmeinert.issuetracker.user;

import de.jmeinert.issuetracker.openapi.NotFoundResponse;
import de.jmeinert.issuetracker.openapi.OkResponse;
import de.jmeinert.issuetracker.openapi.OpenApiConfig;
import de.jmeinert.issuetracker.openapi.RestrictedEndpointResponses;
import de.jmeinert.issuetracker.openapi.BadRequestResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = OpenApiConfig.JWT_BEARER_SECURITY_SCHEME)
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/api/users/{userId}/enabled")
    @Operation(summary = "Enable or disable a user")
    @OkResponse(description = "User enabled status updated")
    @NotFoundResponse(description = "User not found")
    @BadRequestResponse
    @RestrictedEndpointResponses
    public UserResponse changeUserEnabled(
        @Valid @RequestBody ChangeUserEnabledRequest request,
        @PathVariable Long userId
    ) {
        return UserResponse.from(
            userService.changeEnabled(userId, request.enabled())
        );
    }
}
