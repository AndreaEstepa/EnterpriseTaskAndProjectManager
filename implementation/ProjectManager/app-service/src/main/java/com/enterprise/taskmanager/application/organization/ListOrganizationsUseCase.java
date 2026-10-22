package com.enterprise.taskmanager.application.organization;

import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ListOrganizationsUseCase {
    private final UserRepository userRepository;
    private final OrganizationQueryPort organizationQueryPort;
    public ListOrganizationsUseCase(UserRepository userRepository, OrganizationQueryPort organizationQueryPort) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.organizationQueryPort = Objects.requireNonNull(organizationQueryPort);
    }

    public List<OrganizationQueryPort.OrganizationAccess> execute(UUID userId) {
        userRepository.findById(userId)
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .orElseThrow(AccessDeniedException::new);
        return organizationQueryPort.findAllForMember(userId);
    }
}
