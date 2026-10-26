package com.enterprise.taskmanager.application.task;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.project.ProjectRepository;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.task.Task;
import com.enterprise.taskmanager.domain.task.TaskStatus;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class TransitionTaskStatusUseCase {

    private final TaskAccessService access;
    private final TaskRepository tasks;
    private final Clock clock;

    public TransitionTaskStatusUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks,
            Clock clock) {
        this.access = new TaskAccessService(users, projects, organizations, tasks);
        this.tasks = Objects.requireNonNull(tasks);
        this.clock = Objects.requireNonNull(clock);
    }

    public Task execute(TransitionTaskStatusCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        Task task = access.requireActiveTask(command.taskId(), command.requestingUserId());
        return tasks.save(task.transitionTo(command.targetStatus(), clock.instant()));
    }

    public record TransitionTaskStatusCommand(
            UUID requestingUserId,
            UUID taskId,
            TaskStatus targetStatus) {
    }
}
