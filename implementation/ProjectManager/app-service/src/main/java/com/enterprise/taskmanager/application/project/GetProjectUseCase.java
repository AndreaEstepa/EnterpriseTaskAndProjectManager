package com.enterprise.taskmanager.application.project;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.shared.ResourceNotFoundException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.util.Objects;
import java.util.UUID;

public final class GetProjectUseCase {

    private final UserRepository userRepository;
    private final OrganizationQueryPort organizationQueryPort;
    private final ProjectRepository projectRepository;

    public GetProjectUseCase(
            UserRepository userRepository,
            OrganizationQueryPort organizationQueryPort,
            ProjectRepository projectRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.organizationQueryPort = Objects.requireNonNull(organizationQueryPort);
        this.projectRepository = Objects.requireNonNull(projectRepository);
    }

    public Project execute(UUID projectId, UUID requestingUserId) {
        userRepository.findById(requestingUserId)
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Project"));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project"));
        organizationQueryPort.findAccessibleById(project.getOrganizationId(), requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project"));
        return project;
    }
}
