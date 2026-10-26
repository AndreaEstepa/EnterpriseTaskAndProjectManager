package com.enterprise.taskmanager.application.task;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.project.ProjectRepository;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.task.Task;
import com.enterprise.taskmanager.domain.task.TaskPriority;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class UpdateTaskUseCase {

    private final TaskAccessService access;
    private final TaskRepository tasks;
    private final Clock clock;

    public UpdateTaskUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks,
            Clock clock) {
        this.access = new TaskAccessService(users, projects, organizations, tasks);
        this.tasks = Objects.requireNonNull(tasks);
        this.clock = Objects.requireNonNull(clock);
    }

    public Task execute(UpdateTaskCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        Task task = access.requireActiveTask(command.taskId(), command.requestingUserId());
        Task updated = task.updateDetails(
                command.title(), command.description(), command.priority(), clock.instant());
        return tasks.save(updated);
    }

    public record UpdateTaskCommand(
            UUID requestingUserId,
            UUID taskId,
            String title,
            String description,
            TaskPriority priority) {
    }
}
