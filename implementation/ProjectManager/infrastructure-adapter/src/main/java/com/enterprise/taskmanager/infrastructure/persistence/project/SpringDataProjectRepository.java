package com.enterprise.taskmanager.infrastructure.persistence.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataProjectRepository extends JpaRepository<ProjectJpaEntity, UUID> {
    @Query(
            value = "SELECT EXISTS (SELECT 1 FROM projects WHERE organization_id = :organizationId "
                    + "AND lower(btrim(name)) = :name)",
            nativeQuery = true)
    boolean existsByOrganizationIdAndNormalizedName(
            @Param("organizationId") UUID organizationId,
            @Param("name") String name);

    @Query(
            value = "SELECT EXISTS (SELECT 1 FROM projects WHERE organization_id = :organizationId "
                    + "AND id <> :excludedProjectId AND lower(btrim(name)) = :name)",
            nativeQuery = true)
    boolean existsByOrganizationIdAndNormalizedNameExceptProjectId(
            @Param("organizationId") UUID organizationId,
            @Param("name") String name,
            @Param("excludedProjectId") UUID excludedProjectId);

    List<ProjectJpaEntity> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);
}
