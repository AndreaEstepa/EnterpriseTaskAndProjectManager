package com.enterprise.taskmanager.infrastructure.persistence.organization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataOrganizationMemberRepository
        extends JpaRepository<OrganizationMemberJpaEntity, UUID> {
}
