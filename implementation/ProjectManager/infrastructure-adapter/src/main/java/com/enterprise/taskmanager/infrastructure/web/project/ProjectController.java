package com.enterprise.taskmanager.infrastructure.web.project;

import com.enterprise.taskmanager.application.project.CreateProjectUseCase;
import com.enterprise.taskmanager.application.project.ArchiveProjectUseCase;
import com.enterprise.taskmanager.application.project.GetProjectUseCase;
import com.enterprise.taskmanager.application.project.ListProjectsUseCase;
import com.enterprise.taskmanager.application.project.UpdateProjectUseCase;
import com.enterprise.taskmanager.domain.project.Project;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final CreateProjectUseCase createProjectUseCase;
    private final ListProjectsUseCase listProjectsUseCase;
    private final GetProjectUseCase getProjectUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;
    private final ArchiveProjectUseCase archiveProjectUseCase;

    public ProjectController(
            CreateProjectUseCase createProjectUseCase,
            ListProjectsUseCase listProjectsUseCase,
            GetProjectUseCase getProjectUseCase,
            UpdateProjectUseCase updateProjectUseCase,
            ArchiveProjectUseCase archiveProjectUseCase) {
        this.createProjectUseCase = createProjectUseCase;
        this.listProjectsUseCase = listProjectsUseCase;
        this.getProjectUseCase = getProjectUseCase;
        this.updateProjectUseCase = updateProjectUseCase;
        this.archiveProjectUseCase = archiveProjectUseCase;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProjectRequest request) {
        Project project = createProjectUseCase.execute(new CreateProjectUseCase.CreateProjectCommand(userId(jwt), request.organizationId(), request.name(), request.description()));
        ProjectResponse response = ProjectResponse.from(project);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{projectId}").buildAndExpand(project.getId()).toUri()).body(response);
    }

    @GetMapping
    public List<ProjectResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID organizationId) {
        return listProjectsUseCase.execute(userId(jwt), organizationId).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId) {
        return ProjectResponse.from(getProjectUseCase.execute(projectId, userId(jwt)));
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        Project project = updateProjectUseCase.execute(
                new UpdateProjectUseCase.UpdateProjectCommand(
                        userId(jwt), projectId, request.name(), request.description()));
        return ProjectResponse.from(project);
    }

    @PatchMapping("/{projectId}/archive")
    public ProjectResponse archive(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId) {
        return ProjectResponse.from(archiveProjectUseCase.execute(projectId, userId(jwt)));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
