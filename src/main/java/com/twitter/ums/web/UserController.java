package com.twitter.ums.web;

import com.twitter.ums.contracts.AssignRoleRequest;
import com.twitter.ums.contracts.LoginRequest;
import com.twitter.ums.contracts.LoginResponse;
import com.twitter.ums.contracts.MessageResponse;
import com.twitter.ums.contracts.PatchUserRequest;
import com.twitter.ums.contracts.RegisterRequest;
import com.twitter.ums.contracts.RoleAssignmentResponse;
import com.twitter.ums.contracts.RoleResponse;
import com.twitter.ums.contracts.SessionResponse;
import com.twitter.ums.contracts.UpdateUserRequest;
import com.twitter.ums.contracts.UserCreatedResponse;
import com.twitter.ums.contracts.UserDetailsResponse;
import com.twitter.ums.contracts.UserListItem;
import com.twitter.ums.service.UserService;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    public static final String SESSION_HEADER = "X-Session-Id";

    private final RequestValidator validator;
    private final UserService userService;

    public UserController(RequestValidator validator, UserService userService) {
        this.validator = validator;
        this.userService = userService;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserCreatedResponse> register(@RequestBody RegisterRequest request) {
        UserCreatedResponse created = userService.register(validator.validateRegister(request));
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponse login(@RequestBody LoginRequest request) {
        return userService.login(validator.validateLogin(request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public MessageResponse logout(@PathVariable long sessionId) {
        return userService.logout(validator.validateUserId(sessionId));
    }

    @GetMapping("/roles")
    public List<RoleResponse> roles() {
        return userService.listRoles();
    }

    @GetMapping("/users")
    public List<UserListItem> users() {
        return userService.listUsers();
    }

    @GetMapping("/users/{userId}")
    public UserDetailsResponse user(@PathVariable long userId) {
        return userService.getUser(validator.validateUserId(userId));
    }

    @PutMapping(value = "/users/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserDetailsResponse update(@PathVariable long userId, @RequestBody UpdateUserRequest request) {
        return userService.update(validator.validateUserId(userId), validator.validateUpdate(request));
    }

    @PatchMapping(value = "/users/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserDetailsResponse patch(@PathVariable long userId, @RequestBody PatchUserRequest request) {
        return userService.patch(validator.validateUserId(userId), validator.validatePatch(request));
    }

    @DeleteMapping("/users/{userId}")
    public MessageResponse delete(@PathVariable long userId) {
        return userService.delete(validator.validateUserId(userId));
    }

    @PostMapping(value = "/users/{userId}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RoleAssignmentResponse assignRole(
            @PathVariable long userId,
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
            @RequestBody AssignRoleRequest request
    ) {
        return userService.assignRole(
                validator.validateUserId(userId),
                validator.validateAssignRole(request),
                validator.requireSessionId(sessionId)
        );
    }

    @GetMapping("/users/{userId}/sessions/last")
    public SessionResponse lastSession(@PathVariable long userId) {
        return userService.lastSession(validator.validateUserId(userId));
    }
}
