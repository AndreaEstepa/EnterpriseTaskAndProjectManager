package com.enterprise.taskmanager.application.task;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.project.ProjectRepository;
import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.shared.ResourceNotFoundException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.organization.Organization;
import com.enterprise.taskmanager.domain.organization.OrganizationRole;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectStatus;
import com.enterprise.taskmanager.domain.task.Task;
import com.enterprise.taskmanager.domain.task.TaskPriority;
import com.enterprise.taskmanager.domain.task.TaskStatus;
import com.enterprise.taskmanager.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ASSIGNEE_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();

    private final UserRepository users = mock(UserRepository.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final OrganizationQueryPort organizations = mock(OrganizationQueryPort.class);
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final User user = User.register(USER_ID, "member@example.com", "Member User", "hash", NOW);
    private final User assignee = User.register(ASSIGNEE_ID, "assignee@example.com", "Assignee User", "hash", NOW);
    private final Organization organization = Organization.create(ORGANIZATION_ID, "Acme", NOW);
    private final Project project = Project.create(
            PROJECT_ID, ORGANIZATION_ID, "Acme Project", null, NOW);
    private final Task task = Task.create(
            TASK_ID, PROJECT_ID, USER_ID, "Implement tasks", null, TaskPriority.MEDIUM, NOW);

    @Test
    void createsTaskInBacklogForActiveOrganizationMember() {
        allowRequesterOnActiveProject();
        when(tasks.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task created = new CreateTaskUseCase(users, projects, organizations, tasks, clock).execute(
                new CreateTaskUseCase.CreateTaskCommand(
                        USER_ID, PROJECT_ID, "Implement tasks", "Task API", TaskPriority.HIGH, null));

        assertEquals(PROJECT_ID, created.projectId());
        assertEquals(USER_ID, created.createdByUserId());
        assertEquals(TaskStatus.BACKLOG, created.status());
        assertEquals(TaskPriority.HIGH, created.priority());
        verify(tasks).save(created);
    }

    @Test
    void refusesTaskCreationForArchivedProject() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(Project.restore(
                PROJECT_ID, ORGANIZATION_ID, "Acme Project", null, ProjectStatus.ARCHIVED, NOW, NOW)));
        allowMembership(USER_ID, OrganizationRole.MEMBER);

        assertThrows(
                AccessDeniedException.class,
                () -> new CreateTaskUseCase(users, projects, organizations, tasks, clock).execute(
                        new CreateTaskUseCase.CreateTaskCommand(
                                USER_ID, PROJECT_ID, "Implement tasks", null, TaskPriority.MEDIUM, null)));

        verify(tasks, never()).save(any(Task.class));
    }

    @Test
    void refusesAssigneeOutsideOrganization() {
        allowRequesterOnActiveProject();
        when(users.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));
        when(organizations.findAccessibleById(ORGANIZATION_ID, ASSIGNEE_ID)).thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> new CreateTaskUseCase(users, projects, organizations, tasks, clock).execute(
                        new CreateTaskUseCase.CreateTaskCommand(
                                USER_ID, PROJECT_ID, "Implement tasks", null, TaskPriority.MEDIUM, ASSIGNEE_ID)));

        verify(tasks, never()).save(any(Task.class));
    }

    @Test
    void assignsAndUnassignsOnlyActiveOrganizationMembers() {
        allowRequesterOnActiveProject();
        when(tasks.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(users.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));
        allowMembership(ASSIGNEE_ID, OrganizationRole.MEMBER);
        when(tasks.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AssignTaskUseCase useCase = new AssignTaskUseCase(users, projects, organizations, tasks, clock);

        Task assigned = useCase.execute(
                new AssignTaskUseCase.AssignTaskCommand(USER_ID, TASK_ID, ASSIGNEE_ID));
        Task unassigned = useCase.execute(
                new AssignTaskUseCase.AssignTaskCommand(USER_ID, TASK_ID, null));

        assertEquals(ASSIGNEE_ID, assigned.assigneeUserId());
        assertNull(unassigned.assigneeUserId());
        verify(tasks).save(assigned);
        verify(tasks).save(unassigned);
    }

    @Test
    void updatesTaskDetailsForMemberAndRejectsArchivedProject() {
        allowRequesterOnActiveProject();
        when(tasks.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(tasks.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateTaskUseCase useCase = new UpdateTaskUseCase(users, projects, organizations, tasks, clock);

        Task updated = useCase.execute(new UpdateTaskUseCase.UpdateTaskCommand(
                USER_ID, TASK_ID, "Implement task endpoints", "API and persistence", TaskPriority.URGENT));

        assertEquals("Implement task endpoints", updated.title().value());
        assertEquals("API and persistence", updated.description());
        assertEquals(TaskPriority.URGENT, updated.priority());

        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(Project.restore(
                PROJECT_ID, ORGANIZATION_ID, "Acme Project", null, ProjectStatus.ARCHIVED, NOW, NOW)));
        assertThrows(
                AccessDeniedException.class,
                () -> useCase.execute(new UpdateTaskUseCase.UpdateTaskCommand(
                        USER_ID, TASK_ID, "Changed title", null, TaskPriority.LOW)));
    }

    @Test
    void transitionsTaskStatusAndRejectsInvalidJump() {
        allowRequesterOnActiveProject();
        when(tasks.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(tasks.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TransitionTaskStatusUseCase useCase =
                new TransitionTaskStatusUseCase(users, projects, organizations, tasks, clock);

        Task todo = useCase.execute(new TransitionTaskStatusUseCase.TransitionTaskStatusCommand(
                USER_ID, TASK_ID, TaskStatus.TODO));
        assertEquals(TaskStatus.TODO, todo.status());

        assertThrows(
                com.enterprise.taskmanager.domain.shared.DomainValidationException.class,
                () -> useCase.execute(new TransitionTaskStatusUseCase.TransitionTaskStatusCommand(
                        USER_ID, TASK_ID, TaskStatus.DONE)));
    }

    @Test
    void listsAndGetsTasksOnlyForOrganizationMembers() {
        allowRequesterOnActiveProject();
        when(tasks.findAllByProjectId(PROJECT_ID)).thenReturn(List.of(task));
        when(tasks.findById(TASK_ID)).thenReturn(Optional.of(task));

        List<Task> listed = new ListTasksUseCase(users, projects, organizations, tasks)
                .execute(PROJECT_ID, USER_ID);
        Task found = new GetTaskUseCase(users, projects, organizations, tasks)
                .execute(TASK_ID, USER_ID);

        assertEquals(List.of(task), listed);
        assertEquals(TASK_ID, found.id());
    }

    @Test
    void allowsReadingTasksInArchivedProjects() {
        Project archivedProject = Project.restore(
                PROJECT_ID, ORGANIZATION_ID, "Acme Project", null, ProjectStatus.ARCHIVED, NOW, NOW);
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(archivedProject));
        allowMembership(USER_ID, OrganizationRole.MEMBER);
        when(tasks.findAllByProjectId(PROJECT_ID)).thenReturn(List.of(task));
        when(tasks.findById(TASK_ID)).thenReturn(Optional.of(task));

        List<Task> listed = new ListTasksUseCase(users, projects, organizations, tasks)
                .execute(PROJECT_ID, USER_ID);
        Task found = new GetTaskUseCase(users, projects, organizations, tasks)
                .execute(TASK_ID, USER_ID);

        assertEquals(List.of(task), listed);
        assertEquals(TASK_ID, found.id());
    }

    @Test
    void hidesTaskFromUsersOutsideOrganization() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(tasks.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> new GetTaskUseCase(users, projects, organizations, tasks)
                        .execute(TASK_ID, USER_ID));
    }

    @Test
    void reportsMissingTask() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(tasks.findById(TASK_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> new GetTaskUseCase(users, projects, organizations, tasks)
                        .execute(TASK_ID, USER_ID));
    }

    private void allowRequesterOnActiveProject() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        allowMembership(USER_ID, OrganizationRole.MEMBER);
    }

    private void allowMembership(UUID userId, OrganizationRole role) {
        when(organizations.findAccessibleById(ORGANIZATION_ID, userId))
                .thenReturn(Optional.of(new OrganizationQueryPort.OrganizationAccess(organization, role)));
    }
}
