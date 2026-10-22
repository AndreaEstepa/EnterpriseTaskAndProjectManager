package com.enterprise.taskmanager.application.organization;

import com.enterprise.taskmanager.application.shared.ResourceNotFoundException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.util.Objects;
import java.util.UUID;

public final class GetOrganizationUseCase {
    private final UserRepository userRepository;
    private final OrganizationQueryPort organizationQueryPort;

    public GetOrganizationUseCase(UserRepository userRepository, OrganizationQueryPort organizationQueryPort) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.organizationQueryPort = Objects.requireNonNull(organizationQueryPort);
    }

    public OrganizationQueryPort.OrganizationAccess execute(UUID organizationId, UUID requestingUserId) {
        userRepository.findById(requestingUserId)
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        return organizationQueryPort.findAccessibleById(organizationId, requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization"));
    }
}
