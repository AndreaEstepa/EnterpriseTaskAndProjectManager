package com.enterprise.taskmanager.infrastructure.persistence.organization;

import com.enterprise.taskmanager.domain.organization.OrganizationMember;
import com.enterprise.taskmanager.domain.organization.OrganizationRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_members")
public class OrganizationMemberJpaEntity {
    @Id
    private UUID id;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationRole role;
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected OrganizationMemberJpaEntity() {
    }

    public OrganizationMemberJpaEntity(OrganizationMember member) {
        id = member.id();
        organizationId = member.organizationId();
        userId = member.userId();
        role = member.role();
        joinedAt = member.joinedAt();
    }
}
