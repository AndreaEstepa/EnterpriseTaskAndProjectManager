package com.enterprise.taskmanager.infrastructure.persistence.project;

import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectJpaEntity {
    @Id
    private UUID id;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectJpaEntity() {
    }

    public ProjectJpaEntity(Project project) {
        id = project.getId();
        organizationId = project.getOrganizationId();
        name = project.getName().value();
        description = project.getDescription();
        status = project.getStatus();
        createdAt = project.getCreatedAt();
        updatedAt = project.getUpdatedAt();
    }

    public Project toDomain() {
        return Project.restore(id, organizationId, name, description, status, createdAt, updatedAt);
    }
}
