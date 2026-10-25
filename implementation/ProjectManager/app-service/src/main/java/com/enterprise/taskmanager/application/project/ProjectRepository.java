package com.enterprise.taskmanager.application.project;

import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectName;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {
    boolean existsByOrganizationIdAndName(UUID organizationId, ProjectName name);
    boolean existsByOrganizationIdAndNameExceptProjectId(
            UUID organizationId,
            ProjectName name,
            UUID excludedProjectId);
    Optional<Project> findById(UUID projectId);
    List<Project> findAllByOrganizationId(UUID organizationId);
    Project save(Project project);
}
