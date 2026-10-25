package com.enterprise.taskmanager.application.project;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.shared.ProjectNameAlreadyTakenException;
import com.enterprise.taskmanager.application.shared.ResourceNotFoundException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectName;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class UpdateProjectUseCase {

    private final UserRepository userRepository;
    private final OrganizationQueryPort organizationQueryPort;
    private final ProjectRepository projectRepository;
    private final Clock clock;

    public UpdateProjectUseCase(
            UserRepository userRepository,
            OrganizationQueryPort organizationQueryPort,
            ProjectRepository projectRepository,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.organizationQueryPort = Objects.requireNonNull(organizationQueryPort);
        this.projectRepository = Objects.requireNonNull(projectRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Project execute(UpdateProjectCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        userRepository.findById(command.requestingUserId())
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .orElseThrow(AccessDeniedException::new);
        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project"));
        organizationQueryPort.findAccessibleById(project.getOrganizationId(), command.requestingUserId())
                .orElseThrow(AccessDeniedException::new);

        ProjectName name = new ProjectName(command.name());
        if (projectRepository.existsByOrganizationIdAndNameExceptProjectId(
                project.getOrganizationId(), name, project.getId())) {
            throw new ProjectNameAlreadyTakenException();
        }

        Instant now = clock.instant();
        Project renamed = project.rename(name.value(), now);
        Project updated = renamed.updateDescription(command.description(), now);
        return projectRepository.save(updated);
    }

    public record UpdateProjectCommand(
            UUID requestingUserId,
            UUID projectId,
            String name,
            String description) {
    }
}
