package com.enterprise.taskmanager.application.user;

import com.enterprise.taskmanager.application.shared.EmailAlreadyRegisteredException;
import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import com.enterprise.taskmanager.domain.user.Email;
import com.enterprise.taskmanager.domain.user.User;

import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public final class RegisterUserUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public RegisterUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.clock = Objects.requireNonNull(clock);
    }

    public User execute(RegisterUserCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        validatePassword(command.password());
        Email email = new Email(command.email());
        if (userRepository.existsByEmail(email))
            throw new EmailAlreadyRegisteredException();

        String passwordHash = passwordHasher.hash(command.password());
        return userRepository.save(User.register(
                UUID.randomUUID(),
                email.value(),
                command.fullName(),
                passwordHash,
                clock.instant()));
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().noneMatch(character -> !Character.isLetterOrDigit(character))) {
            throw new DomainValidationException(
                    "Password must be 8 to 72 UTF-8 bytes and include uppercase, lowercase, numeric, and special characters");
        }
    }

    public record RegisterUserCommand(String email, String password, String fullName) {
    }
}
