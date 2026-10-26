package com.enterprise.taskmanager.application.task;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.project.ProjectRepository;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.task.Task;

import java.util.UUID;

public final class GetTaskUseCase {

    private final TaskAccessService access;

    public GetTaskUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks) {
        this.access = new TaskAccessService(users, projects, organizations, tasks);
    }

    public Task execute(UUID taskId, UUID requestingUserId) {
        return access.requireAccessibleTask(taskId, requestingUserId);
    }
}
