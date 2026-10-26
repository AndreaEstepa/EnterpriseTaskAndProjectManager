package com.enterprise.taskmanager.application.task;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.project.ProjectRepository;
import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.shared.ResourceNotFoundException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectStatus;
import com.enterprise.taskmanager.domain.task.Task;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.util.Objects;
import java.util.UUID;

final class TaskAccessService {

    private final UserRepository users;
    private final ProjectRepository projects;
    private final OrganizationQueryPort organizations;
    private final TaskRepository tasks;

    TaskAccessService(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks) {
        this.users = Objects.requireNonNull(users);
        this.projects = Objects.requireNonNull(projects);
        this.organizations = Objects.requireNonNull(organizations);
        this.tasks = Objects.requireNonNull(tasks);
    }

    Project requireActiveProjectMember(UUID projectId, UUID userId) {
        Project project = requireAccessibleProjectMember(projectId, userId);
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new AccessDeniedException();
        }
        return project;
    }

    Project requireAccessibleProjectMember(UUID projectId, UUID userId) {
        requireActiveUser(userId);
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project"));
        requireOrganizationMembership(project.getOrganizationId(), userId);
        return project;
    }

    Task requireAccessibleTask(UUID taskId, UUID userId) {
        requireActiveUser(userId);
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task"));
        requireAccessibleProjectMember(task.projectId(), userId);
        return task;
    }

    Task requireActiveTask(UUID taskId, UUID userId) {
        Task task = requireAccessibleTask(taskId, userId);
        if (projects.findById(task.projectId())
                .filter(project -> project.getStatus() == ProjectStatus.ACTIVE)
                .isEmpty()) {
            throw new AccessDeniedException();
        }
        return task;
    }

    void requireActiveOrganizationMember(UUID organizationId, UUID userId) {
        requireActiveUser(userId);
        requireOrganizationMembership(organizationId, userId);
    }

    private void requireActiveUser(UUID userId) {
        users.findById(userId)
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .orElseThrow(AccessDeniedException::new);
    }

    private void requireOrganizationMembership(UUID organizationId, UUID userId) {
        organizations.findAccessibleById(organizationId, userId)
                .orElseThrow(AccessDeniedException::new);
    }

}
