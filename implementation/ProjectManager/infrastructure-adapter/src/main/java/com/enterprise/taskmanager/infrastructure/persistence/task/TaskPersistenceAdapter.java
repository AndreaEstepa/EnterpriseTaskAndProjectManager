package com.enterprise.taskmanager.infrastructure.persistence.task;

import com.enterprise.taskmanager.application.task.TaskRepository;
import com.enterprise.taskmanager.domain.task.Task;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TaskPersistenceAdapter implements TaskRepository {

    private final SpringDataTaskRepository tasks;

    public TaskPersistenceAdapter(SpringDataTaskRepository tasks) {
        this.tasks = tasks;
    }

    @Override
    public Optional<Task> findById(UUID taskId) {
        return tasks.findById(taskId).map(TaskJpaEntity::toDomain);
    }

    @Override
    public List<Task> findAllByProjectId(UUID projectId) {
        return tasks.findAllByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(TaskJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Task save(Task task) {
        return tasks.saveAndFlush(new TaskJpaEntity(task)).toDomain();
    }
}
