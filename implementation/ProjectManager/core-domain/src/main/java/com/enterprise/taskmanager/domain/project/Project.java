package com.enterprise.taskmanager.domain.project;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Project {
    private final UUID id;
    private final UUID organizationId;
    private final ProjectName name;
    private final String description;
    private final ProjectStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Project(UUID id, UUID organizationId, ProjectName name, String description, ProjectStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Project id must not be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Project organization id must not be null");
        this.name = Objects.requireNonNull(name, "Project name must not be null");
        this.description = validateDescription(description);
        this.status = Objects.requireNonNull(status, "Project status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at must not be null");
        if (updatedAt.isBefore(createdAt))
            throw new DomainValidationException("Updated at must not be before created at");
    }

    public static Project create(UUID id, UUID organizationId, String name, String description, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        return new Project(id, organizationId, new ProjectName(name), description, ProjectStatus.ACTIVE, now, now);
    }

    public static Project restore(UUID id, UUID organizationId, String name, String description, ProjectStatus status, Instant createdAt, Instant updatedAt) {
        return new Project(id, organizationId, new ProjectName(name), description, status, createdAt, updatedAt);
    }

    public Project rename(String newName, Instant now) {
        ensureEditable(now);
        return new Project(
                id, organizationId, new ProjectName(newName), description, status, createdAt, now);
    }

    public Project updateDescription(String newDescription, Instant now) {
        ensureEditable(now);
        return new Project(id, organizationId, name, newDescription, status, createdAt, now);
    }

    public Project archive(Instant now) {
        ensureEditable(now);
        return new Project(id, organizationId, name, description, ProjectStatus.ARCHIVED, createdAt, now);
    }

    private void ensureEditable(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (status != ProjectStatus.ACTIVE)
            throw new DomainValidationException("Archived projects cannot be modified");
        if (now.isBefore(updatedAt))
            throw new DomainValidationException("Updated at must not be before the previous update");
    }

    private static String validateDescription(String description) {
        if (description == null)
            return null;
        String normalized = description.trim();
        if (normalized.codePointCount(0, normalized.length()) > 500)
            throw new DomainValidationException("Project description must not exceed 500 characters");
        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public ProjectName getName() {
        return name;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }
}
