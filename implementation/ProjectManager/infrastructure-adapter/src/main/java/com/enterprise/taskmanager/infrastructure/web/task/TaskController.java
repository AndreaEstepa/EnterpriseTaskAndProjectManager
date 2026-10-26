package com.enterprise.taskmanager.infrastructure.web.task;

import com.enterprise.taskmanager.application.task.AssignTaskUseCase;
import com.enterprise.taskmanager.application.task.CreateTaskUseCase;
import com.enterprise.taskmanager.application.task.GetTaskUseCase;
import com.enterprise.taskmanager.application.task.ListTasksUseCase;
import com.enterprise.taskmanager.application.task.TransitionTaskStatusUseCase;
import com.enterprise.taskmanager.application.task.UpdateTaskUseCase;
import com.enterprise.taskmanager.domain.task.Task;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TaskController {

    private final CreateTaskUseCase createTaskUseCase;
    private final ListTasksUseCase listTasksUseCase;
    private final GetTaskUseCase getTaskUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final AssignTaskUseCase assignTaskUseCase;
    private final TransitionTaskStatusUseCase transitionTaskStatusUseCase;

    public TaskController(
            CreateTaskUseCase createTaskUseCase,
            ListTasksUseCase listTasksUseCase,
            GetTaskUseCase getTaskUseCase,
            UpdateTaskUseCase updateTaskUseCase,
            AssignTaskUseCase assignTaskUseCase,
            TransitionTaskStatusUseCase transitionTaskStatusUseCase) {
        this.createTaskUseCase = createTaskUseCase;
        this.listTasksUseCase = listTasksUseCase;
        this.getTaskUseCase = getTaskUseCase;
        this.updateTaskUseCase = updateTaskUseCase;
        this.assignTaskUseCase = assignTaskUseCase;
        this.transitionTaskStatusUseCase = transitionTaskStatusUseCase;
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        Task task = createTaskUseCase.execute(new CreateTaskUseCase.CreateTaskCommand(
                userId(jwt),
                projectId,
                request.title(),
                request.description(),
                request.priorityOrDefault(),
                request.assigneeUserId()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/tasks/{taskId}")
                        .buildAndExpand(task.id())
                        .toUri())
                .body(TaskResponse.from(task));
    }

    @GetMapping("/projects/{projectId}/tasks")
    public List<TaskResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return listTasksUseCase.execute(projectId, userId(jwt)).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId) {
        return TaskResponse.from(getTaskUseCase.execute(taskId, userId(jwt)));
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return TaskResponse.from(updateTaskUseCase.execute(new UpdateTaskUseCase.UpdateTaskCommand(
                userId(jwt), taskId, request.title(), request.description(), request.priority())));
    }

    @PatchMapping("/tasks/{taskId}/assignee")
    public TaskResponse assign(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID taskId,
            @RequestBody AssignTaskRequest request) {
        return TaskResponse.from(assignTaskUseCase.execute(new AssignTaskUseCase.AssignTaskCommand(
                userId(jwt), taskId, request.assigneeUserId())));
    }

    @PatchMapping("/tasks/{taskId}/status")
    public TaskResponse transition(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID taskId,
            @Valid @RequestBody TransitionTaskStatusRequest request) {
        return TaskResponse.from(transitionTaskStatusUseCase.execute(
                new TransitionTaskStatusUseCase.TransitionTaskStatusCommand(
                        userId(jwt), taskId, request.targetStatus())));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
