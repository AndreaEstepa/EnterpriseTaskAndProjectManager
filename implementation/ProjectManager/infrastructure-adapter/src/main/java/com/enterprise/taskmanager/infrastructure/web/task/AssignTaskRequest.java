package com.enterprise.taskmanager.infrastructure.web.task;

import java.util.UUID;

public record AssignTaskRequest(UUID assigneeUserId) {
}
