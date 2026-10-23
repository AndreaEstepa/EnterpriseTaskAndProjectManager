package com.enterprise.taskmanager.application.organization;

import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.organization.Organization;
import com.enterprise.taskmanager.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateOrganizationUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");
    private final UserRepository users = mock(UserRepository.class);
    private final OrganizationProvisioningPort provisioning = mock(OrganizationProvisioningPort.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final CreateOrganizationUseCase useCase = new CreateOrganizationUseCase(users, provisioning, clock);

    @Test
    void provisionsOrganizationForActiveUser() {
        UUID userId = UUID.randomUUID();
        User user = User.register(userId, "pepe@example.com", "pepe Doe", "bcrypt-hash", NOW);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(provisioning.createWithAdmin(any(Organization.class), any(UUID.class), any(Instant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Organization created = useCase.execute(new CreateOrganizationUseCase.CreateOrganizationCommand(userId, "Acme Corporation"));
        assertEquals("Acme Corporation", created.name().value());
        assertEquals(NOW, created.createdAt());
        verify(provisioning).createWithAdmin(created, userId, NOW);
    }

    @Test
    void rejectsMissingOrInactiveCreatorBeforeProvisioning() {
        UUID userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> useCase.execute(new CreateOrganizationUseCase.CreateOrganizationCommand(userId, "Acme Corporation")));
        verify(provisioning, never()).createWithAdmin(any(), any(), any());
    }
}
