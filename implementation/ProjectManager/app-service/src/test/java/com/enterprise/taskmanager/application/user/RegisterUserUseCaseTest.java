package com.enterprise.taskmanager.application.user;

import com.enterprise.taskmanager.application.shared.EmailAlreadyRegisteredException;
import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import com.enterprise.taskmanager.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterUserUseCaseTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC);
    private final RegisterUserUseCase useCase = new RegisterUserUseCase(users, passwordHasher, clock);

    @Test
    void validatesHashesAndPersistsRegistration() {
        when(users.existsByEmail(any())).thenReturn(false);
        when(passwordHasher.hash("Secure123!")).thenReturn("bcrypt-hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = useCase.execute(new RegisterUserUseCase.RegisterUserCommand(
                "  Jane@Example.com ", "Secure123!", " Jane Doe "));

        assertEquals("jane@example.com", created.email().value());
        assertEquals("Jane Doe", created.fullName());
        assertEquals("bcrypt-hash", created.passwordHash());
        assertEquals(clock.instant(), created.createdAt());
        verify(passwordHasher).hash("Secure123!");
        verify(users).save(created);
    }

    @Test
    void duplicateEmailIsRejectedBeforeHashing() {
        when(users.existsByEmail(any())).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> useCase.execute(
                new RegisterUserUseCase.RegisterUserCommand("jane@example.com", "Secure123!", "Jane Doe")));

        verify(passwordHasher, never()).hash(any());
        verify(users, never()).save(any());
    }

    @Test
    void weakPasswordIsRejectedBeforePersistence() {
        assertThrows(DomainValidationException.class, () -> useCase.execute(
                new RegisterUserUseCase.RegisterUserCommand("jane@example.com", "weak", "Jane Doe")));

        verify(users, never()).save(any());
    }
}
