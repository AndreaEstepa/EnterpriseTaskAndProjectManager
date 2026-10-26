package com.enterprise.taskmanager.infrastructure.web.task;

import com.enterprise.taskmanager.domain.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @NotBlank @Size(min = 2, max = 150) String title,
        String description,
        @NotNull TaskPriority priority) {
}
