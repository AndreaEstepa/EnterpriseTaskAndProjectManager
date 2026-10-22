package com.enterprise.taskmanager.application.user;

import com.enterprise.taskmanager.application.shared.InvalidCredentialsException;
import com.enterprise.taskmanager.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class AuthenticateUserUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final AccessTokenIssuer tokenIssuer = mock(AccessTokenIssuer.class);
    private final AuthenticateUserUseCase useCase =
            new AuthenticateUserUseCase(users, passwordHasher, tokenIssuer);

    @Test
    void issuesAccessTokenForValidActiveUser() {
        User user = User.register(UUID.randomUUID(), "jane@example.com", "Jane Doe", "bcrypt-hash", NOW);
        Instant expiresAt = NOW.plusSeconds(900);
        when(users.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("Secure123!", "bcrypt-hash")).thenReturn(true);
        when(tokenIssuer.issueFor(user)).thenReturn(new AccessTokenIssuer.IssuedAccessToken("signed-token", expiresAt));

        AuthenticateUserUseCase.AuthenticationResult result = useCase.execute(
                new AuthenticateUserUseCase.AuthenticateUserCommand("Jane@Example.com", "Secure123!"));

        assertEquals(user.id(), result.userId());
        assertEquals("signed-token", result.accessToken());
        assertEquals(expiresAt, result.expiresAt());
        verify(tokenIssuer).issueFor(user);
    }

    @Test
    void rejectsUnknownUserOrIncorrectPassword() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute(
                new AuthenticateUserUseCase.AuthenticateUserCommand("jane@example.com", "wrong")));

        User user = User.register(UUID.randomUUID(), "jane@example.com", "Jane Doe", "bcrypt-hash", NOW);
        when(users.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong", "bcrypt-hash")).thenReturn(false);
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute(
                new AuthenticateUserUseCase.AuthenticateUserCommand("jane@example.com", "wrong")));
    }
}
