package com.enterprise.taskmanager.application.organization;

import com.enterprise.taskmanager.domain.organization.Organization;

import java.time.Instant;
import java.util.UUID;

public interface OrganizationProvisioningPort {
    Organization createWithAdmin(Organization organization, UUID adminUserId, Instant joinedAt);
}
