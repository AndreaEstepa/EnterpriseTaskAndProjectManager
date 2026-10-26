package com.enterprise.taskmanager.application.task;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.project.ProjectRepository;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.task.Task;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ListTasksUseCase {

    private final TaskAccessService access;
    private final TaskRepository tasks;

    public ListTasksUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks) {
        this.access = new TaskAccessService(users, projects, organizations, tasks);
        this.tasks = Objects.requireNonNull(tasks);
    }

    public List<Task> execute(UUID projectId, UUID requestingUserId) {
        access.requireAccessibleProjectMember(projectId, requestingUserId);
        return tasks.findAllByProjectId(projectId);
    }
}
