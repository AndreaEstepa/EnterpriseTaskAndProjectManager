package com.enterprise.taskmanager.application.project;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ListProjectsUseCase {

    private final UserRepository userRepository;
    private final OrganizationQueryPort organizationQueryPort;
    private final ProjectRepository projectRepository;

    public ListProjectsUseCase(
            UserRepository userRepository,
            OrganizationQueryPort organizationQueryPort,
            ProjectRepository projectRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.organizationQueryPort = Objects.requireNonNull(organizationQueryPort);
        this.projectRepository = Objects.requireNonNull(projectRepository);
    }

    public List<Project> execute(UUID requestingUserId, UUID organizationId) {
        userRepository.findById(requestingUserId)
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .orElseThrow(AccessDeniedException::new);
        organizationQueryPort.findAccessibleById(organizationId, requestingUserId)
                .orElseThrow(AccessDeniedException::new);
        return projectRepository.findAllByOrganizationId(organizationId);
    }
}
