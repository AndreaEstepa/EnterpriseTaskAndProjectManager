package com.enterprise.taskmanager.application.organization;

import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.shared.OrganizationNameAlreadyTakenException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.organization.Organization;
import com.enterprise.taskmanager.domain.user.User;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class CreateOrganizationUseCase {

    private final UserRepository userRepository;
    private final OrganizationProvisioningPort provisioningPort;
    private final Clock clock;

    public CreateOrganizationUseCase(
            UserRepository userRepository,
            OrganizationProvisioningPort provisioningPort,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.provisioningPort = Objects.requireNonNull(provisioningPort);
        this.clock = Objects.requireNonNull(clock);
    }

    public Organization execute(CreateOrganizationCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        User user = userRepository.findById(command.requestingUserId())
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .orElseThrow(AccessDeniedException::new);

        Instant now = clock.instant();
        Organization organization = Organization.create(UUID.randomUUID(), command.name(), now);
        return provisioningPort.createWithAdmin(organization, user.id(), now);
    }

    public record CreateOrganizationCommand(UUID requestingUserId, String name) {
    }
}
