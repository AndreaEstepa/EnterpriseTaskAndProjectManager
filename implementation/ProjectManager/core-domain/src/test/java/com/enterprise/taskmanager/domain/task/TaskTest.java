package com.enterprise.taskmanager.domain.task;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskTest {

    private static final UUID TASK_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID CREATOR_ID = UUID.randomUUID();
    private static final UUID ASSIGNEE_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T10:00:00Z");

    @Test
    void createsTaskInBacklogWithMediumPriorityAndNoAssignee() {
        Task task = Task.create(
                TASK_ID, PROJECT_ID, CREATOR_ID, "  Fix login  ", "  Reproduce the error  ",
                TaskPriority.MEDIUM, CREATED_AT);

        assertEquals(TASK_ID, task.id());
        assertEquals(PROJECT_ID, task.projectId());
        assertEquals(CREATOR_ID, task.createdByUserId());
        assertNull(task.assigneeUserId());
        assertEquals("Fix login", task.title().value());
        assertEquals("Reproduce the error", task.description());
        assertEquals(TaskStatus.BACKLOG, task.status());
        assertEquals(TaskPriority.MEDIUM, task.priority());
        assertEquals(0, task.version());
        assertEquals(CREATED_AT, task.createdAt());
        assertEquals(CREATED_AT, task.updatedAt());
    }

    @Test
    void movesForwardSequentiallyAndAllowsReviewReturn() {
        Task task = Task.create(
                TASK_ID, PROJECT_ID, CREATOR_ID, "Fix login", null, TaskPriority.HIGH, CREATED_AT);

        Task todo = task.transitionTo(TaskStatus.TODO, CREATED_AT.plusSeconds(1));
        Task inProgress = todo.transitionTo(TaskStatus.IN_PROGRESS, CREATED_AT.plusSeconds(2));
        Task inReview = inProgress.transitionTo(TaskStatus.IN_REVIEW, CREATED_AT.plusSeconds(3));
        Task sentBackForChanges = inReview.transitionTo(TaskStatus.IN_PROGRESS, CREATED_AT.plusSeconds(4));
        Task completed = inReview.transitionTo(TaskStatus.DONE, CREATED_AT.plusSeconds(5));

        assertEquals(TaskStatus.TODO, todo.status());
        assertEquals(TaskStatus.IN_PROGRESS, inProgress.status());
        assertEquals(TaskStatus.IN_REVIEW, inReview.status());
        assertEquals(TaskStatus.IN_PROGRESS, sentBackForChanges.status());
        assertEquals(TaskStatus.DONE, completed.status());
        assertEquals(TaskStatus.BACKLOG, task.status());
    }

    @Test
    void rejectsSkippedBackwardAndTerminalTransitions() {
        Task backlog = taskAt(TaskStatus.BACKLOG);
        Task todo = taskAt(TaskStatus.TODO);
        Task inProgress = taskAt(TaskStatus.IN_PROGRESS);
        Task done = taskAt(TaskStatus.DONE);

        assertThrows(DomainValidationException.class, () -> backlog.transitionTo(TaskStatus.DONE, CREATED_AT));
        assertThrows(DomainValidationException.class, () -> todo.transitionTo(TaskStatus.BACKLOG, CREATED_AT));
        assertThrows(DomainValidationException.class, () -> inProgress.transitionTo(TaskStatus.TODO, CREATED_AT));
        assertThrows(DomainValidationException.class, () -> done.transitionTo(TaskStatus.IN_REVIEW, CREATED_AT));
        assertThrows(DomainValidationException.class, () -> backlog.transitionTo(TaskStatus.BACKLOG, CREATED_AT));
    }

    @Test
    void updatesTaskDetailsAndAssignmentImmutably() {
        Task task = Task.create(
                TASK_ID, PROJECT_ID, CREATOR_ID, "Fix login", null, TaskPriority.MEDIUM, CREATED_AT);

        Task assigned = task.assignTo(ASSIGNEE_ID, CREATED_AT.plusSeconds(1));
        Task updated = assigned.updateDetails(
                "Fix login flow", "Handle expired sessions", TaskPriority.URGENT, CREATED_AT.plusSeconds(2));
        Task unassigned = updated.unassign(CREATED_AT.plusSeconds(3));

        assertEquals(ASSIGNEE_ID, assigned.assigneeUserId());
        assertEquals("Fix login flow", updated.title().value());
        assertEquals("Handle expired sessions", updated.description());
        assertEquals(TaskPriority.URGENT, updated.priority());
        assertNull(unassigned.assigneeUserId());
        assertNull(task.assigneeUserId());
        assertEquals("Fix login", task.title().value());
    }

    @Test
    void restoresPersistedAssignmentStatusAndVersion() {
        Task restored = Task.restore(
                TASK_ID,
                PROJECT_ID,
                CREATOR_ID,
                ASSIGNEE_ID,
                "Fix login",
                null,
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                7,
                CREATED_AT,
                CREATED_AT.plusSeconds(10));

        assertEquals(ASSIGNEE_ID, restored.assigneeUserId());
        assertEquals(TaskStatus.IN_PROGRESS, restored.status());
        assertEquals(7, restored.version());
        assertThrows(
                DomainValidationException.class,
                () -> Task.restore(
                        TASK_ID, PROJECT_ID, CREATOR_ID, null, "Fix login", null,
                        TaskStatus.BACKLOG, TaskPriority.MEDIUM, -1, CREATED_AT, CREATED_AT));
        assertThrows(
                DomainValidationException.class,
                () -> taskAt(TaskStatus.BACKLOG).updateDetails(
                        "Updated title", null, TaskPriority.LOW, CREATED_AT.minusSeconds(1)));
    }

    private static Task taskAt(TaskStatus status) {
        return Task.restore(
                TASK_ID, PROJECT_ID, CREATOR_ID, null, "Task title", null,
                status, TaskPriority.MEDIUM, 0, CREATED_AT, CREATED_AT);
    }
}
