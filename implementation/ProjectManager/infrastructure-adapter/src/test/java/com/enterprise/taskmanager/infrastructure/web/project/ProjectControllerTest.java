package com.enterprise.taskmanager.infrastructure.web.project;

import com.enterprise.taskmanager.application.project.CreateProjectUseCase;
import com.enterprise.taskmanager.application.project.ArchiveProjectUseCase;
import com.enterprise.taskmanager.application.project.GetProjectUseCase;
import com.enterprise.taskmanager.application.project.ListProjectsUseCase;
import com.enterprise.taskmanager.application.project.UpdateProjectUseCase;
import com.enterprise.taskmanager.application.shared.ProjectNameAlreadyTakenException;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectStatus;
import com.enterprise.taskmanager.infrastructure.web.ApiExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
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

import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ProjectControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");

    private final CreateProjectUseCase createProjectUseCase = mock(CreateProjectUseCase.class);
    private final ListProjectsUseCase listProjectsUseCase = mock(ListProjectsUseCase.class);
    private final GetProjectUseCase getProjectUseCase = mock(GetProjectUseCase.class);
    private final UpdateProjectUseCase updateProjectUseCase = mock(UpdateProjectUseCase.class);
    private final ArchiveProjectUseCase archiveProjectUseCase = mock(ArchiveProjectUseCase.class);

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

    private final MockMvc mockMvc;

    ProjectControllerTest() {
        mockMvc = standaloneSetup(new ProjectController(
                        createProjectUseCase,
                        listProjectsUseCase,
                        getProjectUseCase,
                        updateProjectUseCase,
                        archiveProjectUseCase))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        new ObjectMapper()
                                .registerModule(new JavaTimeModule())
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)))
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(jwtPrincipalResolver)
                .build();
    }

    @Test
    void createsProjectFromAuthenticatedRequest() throws Exception {
        Project project = Project.create(
                PROJECT_ID, ORGANIZATION_ID, "My Project", "Project description", NOW);
        when(createProjectUseCase.execute(any())).thenReturn(project);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "name": "My Project",
                                  "description": "Project description"
                                }
                                """.formatted(ORGANIZATION_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.organizationId").value(ORGANIZATION_ID.toString()))
                .andExpect(jsonPath("$.name").value("My Project"))
                .andExpect(jsonPath("$.description").value("Project description"))
                .andExpect(jsonPath("$.status").value(ProjectStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").value(NOW.toString()))
                .andExpect(jsonPath("$.updatedAt").value(NOW.toString()));

        verify(createProjectUseCase).execute(
                new CreateProjectUseCase.CreateProjectCommand(
                        USER_ID, ORGANIZATION_ID, "My Project", "Project description"));
    }

    @Test
    void rejectsInvalidRequestBeforeCallingUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " "
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(createProjectUseCase, never()).execute(any());
    }

    @Test
    void mapsDuplicateProjectNameToConflict() throws Exception {
        when(createProjectUseCase.execute(any())).thenThrow(new ProjectNameAlreadyTakenException());

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "name": "My Project"
                                }
                                """.formatted(ORGANIZATION_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }
}
