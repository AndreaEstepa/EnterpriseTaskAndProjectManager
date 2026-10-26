package com.enterprise.taskmanager.application.task;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.project.ProjectRepository;
import com.enterprise.taskmanager.application.shared.ResourceNotFoundException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.task.Task;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class AssignTaskUseCase {

    private final TaskAccessService access;
    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final Clock clock;

    public AssignTaskUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks,
            Clock clock) {
        this.access = new TaskAccessService(users, projects, organizations, tasks);
        this.tasks = Objects.requireNonNull(tasks);
        this.projects = Objects.requireNonNull(projects);
        this.clock = Objects.requireNonNull(clock);
    }

    public Task execute(AssignTaskCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        Task task = access.requireActiveTask(command.taskId(), command.requestingUserId());
        Task updated;
        if (command.assigneeUserId() == null) {
            updated = task.unassign(clock.instant());
        } else {
            var project = projects.findById(task.projectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project"));
            access.requireActiveOrganizationMember(project.getOrganizationId(), command.assigneeUserId());
            updated = task.assignTo(command.assigneeUserId(), clock.instant());
        }
        return tasks.save(updated);
    }

    public record AssignTaskCommand(UUID requestingUserId, UUID taskId, UUID assigneeUserId) {
    }
}
