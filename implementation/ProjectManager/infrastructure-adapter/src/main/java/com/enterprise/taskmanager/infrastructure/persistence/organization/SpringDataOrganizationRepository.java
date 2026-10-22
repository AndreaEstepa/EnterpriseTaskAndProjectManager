package com.enterprise.taskmanager.infrastructure.persistence.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataOrganizationRepository extends JpaRepository<OrganizationJpaEntity, UUID> {
    @Query(value = "SELECT EXISTS (SELECT 1 FROM organizations WHERE lower(btrim(name)) = :name)", nativeQuery = true)
    boolean existsByNormalizedName(@Param("name") String name);

    @Query(value = """
            SELECT o.id AS "organizationId", o.name AS "organizationName",
                   o.created_at AS "createdAt", o.updated_at AS "updatedAt", m.role AS "role"
            FROM organizations o
            JOIN organization_members m ON m.organization_id = o.id
            WHERE m.user_id = :userId
            ORDER BY o.name
            """, nativeQuery = true)
    List<OrganizationAccessProjection> findAllForMember(@Param("userId") UUID userId);

    @Query(value = """
            SELECT o.id AS "organizationId", o.name AS "organizationName",
                   o.created_at AS "createdAt", o.updated_at AS "updatedAt", m.role AS "role"
            FROM organizations o
            JOIN organization_members m ON m.organization_id = o.id
            WHERE o.id = :organizationId AND m.user_id = :userId
            """, nativeQuery = true)
    Optional<OrganizationAccessProjection> findAccessibleById(
            @Param("organizationId") UUID organizationId,
            @Param("userId") UUID userId);
}
