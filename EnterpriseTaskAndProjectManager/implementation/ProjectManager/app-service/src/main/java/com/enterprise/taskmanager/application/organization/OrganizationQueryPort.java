package com.enterprise.taskmanager.application.organization;

import com.enterprise.taskmanager.domain.organization.Organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationQueryPort {

    List<Organization> findAllForMember(UUID userId);

    Optional<Organization> findAccessibleById(UUID organizationId, UUID userId);
}
