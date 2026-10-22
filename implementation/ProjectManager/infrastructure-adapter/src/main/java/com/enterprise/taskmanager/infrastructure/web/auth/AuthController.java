package com.enterprise.taskmanager.infrastructure.web.auth;

import com.enterprise.taskmanager.application.user.AuthenticateUserUseCase;
import com.enterprise.taskmanager.application.user.RegisterUserUseCase;
import com.enterprise.taskmanager.domain.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUser;
    private final AuthenticateUserUseCase authenticateUser;

    public AuthController(RegisterUserUseCase registerUser, AuthenticateUserUseCase authenticateUser) {
        this.registerUser = registerUser;
        this.authenticateUser = authenticateUser;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        User user = registerUser.execute(new RegisterUserUseCase.RegisterUserCommand(
                request.email(), request.password(), request.fullName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public AccessTokenResponse login(@Valid @RequestBody LoginRequest request) {
        AuthenticateUserUseCase.AuthenticationResult result = authenticateUser.execute(
                new AuthenticateUserUseCase.AuthenticateUserCommand(request.email(), request.password()));
        return new AccessTokenResponse(result.accessToken(), "Bearer", result.expiresAt());
    }
}
