package com.enterprise.taskmanager.infrastructure.web.task;

import com.enterprise.taskmanager.domain.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionTaskStatusRequest(@NotNull TaskStatus targetStatus) {
}
