package com.enterprise.taskmanager.infrastructure.persistence.task;

import com.enterprise.taskmanager.domain.task.Task;
import com.enterprise.taskmanager.domain.task.TaskPriority;
import com.enterprise.taskmanager.domain.task.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskPersistenceAdapterTest {

    private static final UUID TASK_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

    private final SpringDataTaskRepository repository = mock(SpringDataTaskRepository.class);
    private final TaskPersistenceAdapter adapter = new TaskPersistenceAdapter(repository);

    @Test
    void savesTaskAndMapsTheVersionReturnedByPersistence() {
        Task task = Task.create(TASK_ID, PROJECT_ID, USER_ID, "Review task", "Details", TaskPriority.HIGH, NOW);
        when(repository.saveAndFlush(any(TaskJpaEntity.class))).thenAnswer(invocation -> {
            TaskJpaEntity entity = invocation.getArgument(0);
            Task persisted = Task.restore(
                    task.id(),
                    task.projectId(),
                    task.createdByUserId(),
                    task.assigneeUserId(),
                    task.title().value(),
                    task.description(),
                    TaskStatus.BACKLOG,
                    task.priority(),
                    1,
                    task.createdAt(),
                    task.updatedAt());
            return new TaskJpaEntity(persisted);
        });

        Task saved = adapter.save(task);

        assertEquals(TASK_ID, saved.id());
        assertEquals(TaskStatus.BACKLOG, saved.status());
        assertEquals(TaskPriority.HIGH, saved.priority());
        assertEquals(1, saved.version());
        verify(repository).saveAndFlush(any(TaskJpaEntity.class));
    }

    @Test
    void findsAndListsTasksByProject() {
        Task task = Task.create(TASK_ID, PROJECT_ID, USER_ID, "Review task", null, TaskPriority.MEDIUM, NOW);
        TaskJpaEntity entity = new TaskJpaEntity(task);
        when(repository.findById(TASK_ID)).thenReturn(Optional.of(entity));
        when(repository.findAllByProjectIdOrderByCreatedAtAsc(PROJECT_ID)).thenReturn(List.of(entity));

        assertEquals(task.id(), adapter.findById(TASK_ID).orElseThrow().id());
        assertEquals(List.of(task.id()), adapter.findAllByProjectId(PROJECT_ID).stream().map(Task::id).toList());
        verify(repository).findAllByProjectIdOrderByCreatedAtAsc(PROJECT_ID);
    }
}
