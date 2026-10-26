package com.enterprise.taskmanager.infrastructure.persistence.task;

import com.enterprise.taskmanager.domain.task.Task;
import com.enterprise.taskmanager.domain.task.TaskPriority;
import com.enterprise.taskmanager.domain.task.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class TaskJpaEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "assignee_user_id")
    private UUID assigneeUserId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskJpaEntity() {
    }

    public TaskJpaEntity(Task task) {
        id = task.id();
        projectId = task.projectId();
        createdByUserId = task.createdByUserId();
        assigneeUserId = task.assigneeUserId();
        title = task.title().value();
        description = task.description();
        status = task.status();
        priority = task.priority();
        version = task.version();
        createdAt = task.createdAt();
        updatedAt = task.updatedAt();
    }

    public Task toDomain() {
        return Task.restore(
                id,
                projectId,
                createdByUserId,
                assigneeUserId,
                title,
                description,
                status,
                priority,
                version,
                createdAt,
                updatedAt);
    }
}
