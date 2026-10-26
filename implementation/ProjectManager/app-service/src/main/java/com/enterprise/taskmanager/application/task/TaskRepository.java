package com.enterprise.taskmanager.application.task;

import com.enterprise.taskmanager.domain.task.Task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    Optional<Task> findById(UUID taskId);
    List<Task> findAllByProjectId(UUID projectId);
    Task save(Task task);
}
