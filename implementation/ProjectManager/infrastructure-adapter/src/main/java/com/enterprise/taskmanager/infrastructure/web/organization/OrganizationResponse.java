package com.enterprise.taskmanager.infrastructure.web.organization;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort.OrganizationAccess;
import com.enterprise.taskmanager.domain.organization.Organization;
import com.enterprise.taskmanager.domain.organization.OrganizationRole;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        OrganizationRole userRole,
        Instant createdAt,
        Instant updatedAt) {

    public static OrganizationResponse from(Organization organization, OrganizationRole role) {
        return new OrganizationResponse(
                organization.id(),
                organization.name().value(),
                role,
                organization.createdAt(),
                organization.updatedAt());
    }

    public static OrganizationResponse from(OrganizationAccess access) {
        return from(access.organization(), access.role());
    }
}
