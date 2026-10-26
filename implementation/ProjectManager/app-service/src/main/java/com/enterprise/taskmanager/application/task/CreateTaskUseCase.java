package com.enterprise.taskmanager.application.task;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.project.ProjectRepository;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.task.Task;
import com.enterprise.taskmanager.domain.task.TaskPriority;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class CreateTaskUseCase {

    private final TaskAccessService access;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final Clock clock;

    public CreateTaskUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks,
            Clock clock) {
        this.access = new TaskAccessService(users, projects, organizations, tasks);
        this.projects = Objects.requireNonNull(projects);
        this.tasks = Objects.requireNonNull(tasks);
        this.clock = Objects.requireNonNull(clock);
    }

    public Task execute(CreateTaskCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        Project project = access.requireActiveProjectMember(command.projectId(), command.requestingUserId());
        if (command.assigneeUserId() != null) {
            access.requireActiveOrganizationMember(project.getOrganizationId(), command.assigneeUserId());
        }

        Instant now = clock.instant();
        Task task = Task.create(
                UUID.randomUUID(),
                command.projectId(),
                command.requestingUserId(),
                command.title(),
                command.description(),
                command.priority(),
                now);
        if (command.assigneeUserId() != null) {
            task = task.assignTo(command.assigneeUserId(), now);
        }
        return tasks.save(task);
    }

    public record CreateTaskCommand(
            UUID requestingUserId,
            UUID projectId,
            String title,
            String description,
            TaskPriority priority,
            UUID assigneeUserId) {
    }
}
