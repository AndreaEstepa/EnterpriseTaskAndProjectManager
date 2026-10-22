package com.enterprise.taskmanager.application.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationQueryPort {
    List<OrganizationAccess> findAllForMember(UUID userId);
    Optional<OrganizationAccess> findAccessibleById(UUID organizationId, UUID userId);
    record OrganizationAccess(
            com.enterprise.taskmanager.domain.organization.Organization organization,
            com.enterprise.taskmanager.domain.organization.OrganizationRole role) {
    }
}
