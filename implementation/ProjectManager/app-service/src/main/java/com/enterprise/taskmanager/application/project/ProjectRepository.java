package com.enterprise.taskmanager.application.project;

import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectName;

import java.util.UUID;

public interface ProjectRepository {
    boolean existsByOrganizationIdAndName(UUID organizationId, ProjectName name);
    Project save(Project project);
}
