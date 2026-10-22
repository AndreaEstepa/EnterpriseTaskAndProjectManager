package com.enterprise.taskmanager.domain.organization;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import com.enterprise.taskmanager.domain.user.User;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class OrganizationMember {

    private final UUID id;
    private final UUID organizationId;
    private final UUID userId;
    private final OrganizationRole role;
    private final Instant joinedAt;

    private OrganizationMember(
            UUID id,
            UUID organizationId,
            UUID userId,
            OrganizationRole role,
            Instant joinedAt) {
        this.id = Objects.requireNonNull(id, "Membership id must not be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization id must not be null");
        this.userId = Objects.requireNonNull(userId, "User id must not be null");
        this.role = Objects.requireNonNull(role, "Organization role must not be null");
        this.joinedAt = Objects.requireNonNull(joinedAt, "Joined at must not be null");
    }

    public static OrganizationMember create(
            UUID id,
            Organization organization,
            User user,
            OrganizationRole role,
            Instant joinedAt) {
        Objects.requireNonNull(organization, "Organization must not be null");
        Objects.requireNonNull(user, "User must not be null");
        if (user.status() != UserStatus.ACTIVE) {
            throw new DomainValidationException("Inactive users cannot join an organization");
        }
        return new OrganizationMember(id, organization.id(), user.id(), role, joinedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID userId() {
        return userId;
    }

    public OrganizationRole role() {
        return role;
    }

    public Instant joinedAt() {
        return joinedAt;
    }
}
