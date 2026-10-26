package com.enterprise.taskmanager.infrastructure.web.task;

import com.enterprise.taskmanager.application.task.AssignTaskUseCase;
import com.enterprise.taskmanager.application.task.CreateTaskUseCase;
import com.enterprise.taskmanager.application.task.GetTaskUseCase;
import com.enterprise.taskmanager.application.task.ListTasksUseCase;
import com.enterprise.taskmanager.application.task.TransitionTaskStatusUseCase;
import com.enterprise.taskmanager.application.task.UpdateTaskUseCase;
import com.enterprise.taskmanager.domain.task.Task;
import com.enterprise.taskmanager.domain.task.TaskPriority;
import com.enterprise.taskmanager.domain.task.TaskStatus;
import com.enterprise.taskmanager.infrastructure.web.ApiExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class TaskControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

    private final CreateTaskUseCase createTaskUseCase = mock(CreateTaskUseCase.class);
    private final ListTasksUseCase listTasksUseCase = mock(ListTasksUseCase.class);
    private final GetTaskUseCase getTaskUseCase = mock(GetTaskUseCase.class);
    private final UpdateTaskUseCase updateTaskUseCase = mock(UpdateTaskUseCase.class);
    private final AssignTaskUseCase assignTaskUseCase = mock(AssignTaskUseCase.class);
    private final TransitionTaskStatusUseCase transitionTaskStatusUseCase =
            mock(TransitionTaskStatusUseCase.class);

    private final Jwt jwt = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .subject(USER_ID.toString())
            .build();

    private final HandlerMethodArgumentResolver jwtPrincipalResolver = new HandlerMethodArgumentResolver() {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && parameter.getParameterType().equals(Jwt.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            return jwt;
        }
    };

    private final MockMvc mockMvc = standaloneSetup(new TaskController(
                    createTaskUseCase,
                    listTasksUseCase,
                    getTaskUseCase,
                    updateTaskUseCase,
                    assignTaskUseCase,
                    transitionTaskStatusUseCase))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(
                    new ObjectMapper()
                            .registerModule(new JavaTimeModule())
                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)))
            .setControllerAdvice(new ApiExceptionHandler())
            .setCustomArgumentResolvers(jwtPrincipalResolver)
            .build();

    @Test
    void createsTaskUsingAuthenticatedUserAndDefaultsPriority() throws Exception {
        Task task = Task.create(TASK_ID, PROJECT_ID, USER_ID, "Task title", null, TaskPriority.MEDIUM, NOW);
        when(createTaskUseCase.execute(any())).thenReturn(task);

        mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Task title"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.createdByUserId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.title").value("Task title"))
                .andExpect(jsonPath("$.status").value("BACKLOG"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.version").value(0));

        verify(createTaskUseCase).execute(new CreateTaskUseCase.CreateTaskCommand(
                USER_ID, PROJECT_ID, "Task title", null, TaskPriority.MEDIUM, null));
    }

    @Test
    void rejectsBlankTaskTitleBeforeCallingUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" "}
                                """))
                .andExpect(status().isBadRequest());

        verify(createTaskUseCase, never()).execute(any());
    }

    @Test
    void returnsConflictWhenTaskWasModifiedConcurrently() throws Exception {
        when(transitionTaskStatusUseCase.execute(any()))
                .thenThrow(new OptimisticLockingFailureException("stale task version"));

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/status", TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus":"TODO"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "The task was modified by another request. Reload it and retry."));
    }
}
