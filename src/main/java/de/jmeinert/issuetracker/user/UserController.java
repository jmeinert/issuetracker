package de.jmeinert.issuetracker.user;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/api/users/{userId}/enabled")
    public UserResponse changeUserEnabled(
        @Valid @RequestBody ChangeUserEnabledRequest request,
        @PathVariable Long userId
    ) {
        return UserResponse.from(
            userService.changeEnabled(userId, request.enabled())
        );
    }
}
