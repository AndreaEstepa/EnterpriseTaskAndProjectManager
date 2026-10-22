package com.enterprise.taskmanager.application.user;

import com.enterprise.taskmanager.application.shared.InvalidCredentialsException;
import com.enterprise.taskmanager.domain.user.Email;
import com.enterprise.taskmanager.domain.user.User;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.util.Objects;

public final class AuthenticateUserUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    public AuthenticateUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
    }

    public AuthenticationResult execute(AuthenticateUserCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        User user = userRepository.findByEmail(new Email(command.email()))
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .filter(candidate -> passwordHasher.matches(command.password(), candidate.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        AccessTokenIssuer.IssuedAccessToken token = accessTokenIssuer.issueFor(user);
        return new AuthenticationResult(token.value(), token.expiresAt(), user.id());
    }

    public record AuthenticateUserCommand(String email, String password) {
    }

    public record AuthenticationResult(String accessToken, java.time.Instant expiresAt, java.util.UUID userId) {
    }
}
