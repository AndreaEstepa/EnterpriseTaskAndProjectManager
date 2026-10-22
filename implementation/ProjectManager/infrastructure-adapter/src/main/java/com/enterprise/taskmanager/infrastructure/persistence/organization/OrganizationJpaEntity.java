package com.enterprise.taskmanager.infrastructure.persistence.organization;

import com.enterprise.taskmanager.domain.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class OrganizationJpaEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationJpaEntity() {
    }

    public OrganizationJpaEntity(Organization organization) {
        id = organization.id();
        name = organization.name().value();
        createdAt = organization.createdAt();
        updatedAt = organization.updatedAt();
    }

    public Organization toDomain() {
        return Organization.restore(id, name, createdAt, updatedAt);
    }
}
