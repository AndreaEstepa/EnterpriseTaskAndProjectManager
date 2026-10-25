package com.enterprise.taskmanager.application.project;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.shared.ResourceNotFoundException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.organization.OrganizationRole;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class ArchiveProjectUseCase {

    private final UserRepository userRepository;
    private final OrganizationQueryPort organizationQueryPort;
    private final ProjectRepository projectRepository;
    private final Clock clock;

    public ArchiveProjectUseCase(
            UserRepository userRepository,
            OrganizationQueryPort organizationQueryPort,
            ProjectRepository projectRepository,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.organizationQueryPort = Objects.requireNonNull(organizationQueryPort);
        this.projectRepository = Objects.requireNonNull(projectRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Project execute(UUID projectId, UUID requestingUserId) {
        userRepository.findById(requestingUserId)
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .orElseThrow(AccessDeniedException::new);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project"));
        OrganizationQueryPort.OrganizationAccess access = organizationQueryPort
                .findAccessibleById(project.getOrganizationId(), requestingUserId)
                .orElseThrow(AccessDeniedException::new);
        if (access.role() != OrganizationRole.ADMIN) {
            throw new AccessDeniedException();
        }
        return projectRepository.save(project.archive(clock.instant()));
    }
}
