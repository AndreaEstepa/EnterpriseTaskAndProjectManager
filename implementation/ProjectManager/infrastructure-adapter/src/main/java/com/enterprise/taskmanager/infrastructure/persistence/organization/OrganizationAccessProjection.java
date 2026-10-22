package com.enterprise.taskmanager.infrastructure.persistence.organization;

import java.time.Instant;
import java.util.UUID;

public interface OrganizationAccessProjection {
    UUID getOrganizationId();
    String getOrganizationName();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    String getRole();
}
