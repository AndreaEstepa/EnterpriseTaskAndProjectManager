package com.enterprise.taskmanager.application.project;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.shared.ProjectNameAlreadyTakenException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectName;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class CreateProjectUseCase {
    private final UserRepository userRepository;
    private final OrganizationQueryPort organizationQueryPort;
    private final ProjectRepository projectRepository;
    private final Clock clock;

    public CreateProjectUseCase(UserRepository userRepository, OrganizationQueryPort organizationQueryPort, ProjectRepository projectRepository, Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.organizationQueryPort = Objects.requireNonNull(organizationQueryPort);
        this.projectRepository = Objects.requireNonNull(projectRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Project execute(CreateProjectCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        userRepository.findById(command.requestingUserId()).filter(user -> user.status() == UserStatus.ACTIVE).orElseThrow(AccessDeniedException::new);
        organizationQueryPort.findAccessibleById(command.organizationId(), command.requestingUserId()).orElseThrow(AccessDeniedException::new);

        ProjectName name = new ProjectName(command.name());
        if (projectRepository.existsByOrganizationIdAndName(command.organizationId(), name))
            throw new ProjectNameAlreadyTakenException();
        Project project = Project.create(UUID.randomUUID(), command.organizationId(), name.value(), command.description(), clock.instant());
        return projectRepository.save(project);
    }

    public record CreateProjectCommand(UUID requestingUserId, UUID organizationId, String name, String description) {
    }
}
