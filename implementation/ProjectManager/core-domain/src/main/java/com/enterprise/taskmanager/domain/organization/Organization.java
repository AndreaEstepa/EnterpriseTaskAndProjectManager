package com.enterprise.taskmanager.domain.organization;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Organization {

    private final UUID id;
    private final OrganizationName name;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Organization(UUID id, OrganizationName name, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Organization id must not be null");
        this.name = Objects.requireNonNull(name, "Organization name must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at must not be null");

        if (updatedAt.isBefore(createdAt)) {
            throw new DomainValidationException("Updated at must not be before created at");
        }
    }

    public static Organization create(UUID id, String name, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        return new Organization(id, new OrganizationName(name), now, now);
    }

    public static Organization restore(UUID id, String name, Instant createdAt, Instant updatedAt) {
        return new Organization(id, new OrganizationName(name), createdAt, updatedAt);
    }

    public Organization rename(String newName, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (now.isBefore(updatedAt)) {
            throw new DomainValidationException("Updated at must not be before the previous update");
        }
        return new Organization(id, new OrganizationName(newName), createdAt, now);
    }

    public UUID id() {
        return id;
    }

    public OrganizationName name() {
        return name;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
