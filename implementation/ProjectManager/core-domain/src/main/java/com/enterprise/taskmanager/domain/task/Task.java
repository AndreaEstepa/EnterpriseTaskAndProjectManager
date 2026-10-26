package com.enterprise.taskmanager.domain.task;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Task {

    private final UUID id;
    private final UUID projectId;
    private final UUID createdByUserId;
    private final UUID assigneeUserId;
    private final TaskTitle title;
    private final String description;
    private final TaskStatus status;
    private final TaskPriority priority;
    private final int version;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Task(
            UUID id,
            UUID projectId,
            UUID createdByUserId,
            UUID assigneeUserId,
            TaskTitle title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            int version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Task id must not be null");
        this.projectId = Objects.requireNonNull(projectId, "Task project id must not be null");
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "Task creator id must not be null");
        this.assigneeUserId = assigneeUserId;
        this.title = Objects.requireNonNull(title, "Task title must not be null");
        this.description = description == null ? null : description.trim();
        this.status = Objects.requireNonNull(status, "Task status must not be null");
        this.priority = Objects.requireNonNull(priority, "Task priority must not be null");
        if (version < 0) {
            throw new DomainValidationException("Task version must not be negative");
        }
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "Created at must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new DomainValidationException("Updated at must not be before created at");
        }
    }

    public static Task create(
            UUID id,
            UUID projectId,
            UUID createdByUserId,
            String title,
            String description,
            TaskPriority priority,
            Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        return new Task(
                id,
                projectId,
                createdByUserId,
                null,
                new TaskTitle(title),
                description,
                TaskStatus.BACKLOG,
                priority,
                0,
                now,
                now);
    }

    public static Task restore(
            UUID id,
            UUID projectId,
            UUID createdByUserId,
            UUID assigneeUserId,
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            int version,
            Instant createdAt,
            Instant updatedAt) {
        return new Task(
                id,
                projectId,
                createdByUserId,
                assigneeUserId,
                new TaskTitle(title),
                description,
                status,
                priority,
                version,
                createdAt,
                updatedAt);
    }

    public Task updateDetails(String newTitle, String newDescription, TaskPriority newPriority, Instant now) {
        ensureEditableAt(now);
        return new Task(
                id,
                projectId,
                createdByUserId,
                assigneeUserId,
                new TaskTitle(newTitle),
                newDescription,
                status,
                newPriority,
                version,
                createdAt,
                now);
    }

    public Task assignTo(UUID userId, Instant now) {
        ensureEditableAt(now);
        return new Task(
                id,
                projectId,
                createdByUserId,
                userId,
                title,
                description,
                status,
                priority,
                version,
                createdAt,
                now);
    }

    public Task unassign(Instant now) {
        return assignTo(null, now);
    }

    public Task transitionTo(TaskStatus targetStatus, Instant now) {
        ensureEditableAt(now);
        Objects.requireNonNull(targetStatus, "Target task status must not be null");
        boolean allowed = switch (status) {
            case BACKLOG -> targetStatus == TaskStatus.TODO;
            case TODO -> targetStatus == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> targetStatus == TaskStatus.IN_REVIEW;
            case IN_REVIEW -> targetStatus == TaskStatus.DONE || targetStatus == TaskStatus.IN_PROGRESS;
            case DONE -> false;
        };
        if (!allowed) {
            throw new DomainValidationException(
                    "Task status transition from " + status + " to " + targetStatus + " is not allowed");
        }
        return new Task(
                id,
                projectId,
                createdByUserId,
                assigneeUserId,
                title,
                description,
                targetStatus,
                priority,
                version,
                createdAt,
                now);
    }

    private void ensureEditableAt(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (now.isBefore(updatedAt)) {
            throw new DomainValidationException("Updated at must not be before the previous update");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID projectId() {
        return projectId;
    }

    public UUID createdByUserId() {
        return createdByUserId;
    }

    public UUID assigneeUserId() {
        return assigneeUserId;
    }

    public TaskTitle title() {
        return title;
    }

    public String description() {
        return description;
    }

    public TaskStatus status() {
        return status;
    }

    public TaskPriority priority() {
        return priority;
    }

    public int version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
