package com.enterprise.taskmanager.infrastructure.web.project;

import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID organizationId,
        String name,
        String description,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getOrganizationId(),
                project.getName().value(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
