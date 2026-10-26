package com.enterprise.taskmanager.infrastructure.web.task;

import com.enterprise.taskmanager.domain.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank @Size(min = 2, max = 150) String title,
        String description,
        TaskPriority priority,
        UUID assigneeUserId) {

    TaskPriority priorityOrDefault() {
        return priority == null ? TaskPriority.MEDIUM : priority;
    }
}
