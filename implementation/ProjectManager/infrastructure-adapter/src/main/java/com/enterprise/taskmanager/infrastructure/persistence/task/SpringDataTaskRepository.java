package com.enterprise.taskmanager.infrastructure.persistence.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataTaskRepository extends JpaRepository<TaskJpaEntity, UUID> {
    List<TaskJpaEntity> findAllByProjectIdOrderByCreatedAtAsc(UUID projectId);
}
