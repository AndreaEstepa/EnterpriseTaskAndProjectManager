package com.enterprise.taskmanager.infrastructure.web.task;

import com.enterprise.taskmanager.domain.task.Task;
import com.enterprise.taskmanager.domain.task.TaskPriority;
import com.enterprise.taskmanager.domain.task.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
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

    static TaskResponse from(Task task) {
        return new TaskResponse(
                task.id(),
                task.projectId(),
                task.createdByUserId(),
                task.assigneeUserId(),
                task.title().value(),
                task.description(),
                task.status(),
                task.priority(),
                task.version(),
                task.createdAt(),
                task.updatedAt());
    }
}
