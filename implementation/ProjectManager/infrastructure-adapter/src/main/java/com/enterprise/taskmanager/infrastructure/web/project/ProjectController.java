package com.enterprise.taskmanager.infrastructure.web.project;

import com.enterprise.taskmanager.application.project.CreateProjectUseCase;
import com.enterprise.taskmanager.domain.project.Project;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final CreateProjectUseCase createProjectUseCase;

    public ProjectController(CreateProjectUseCase createProjectUseCase) {
        this.createProjectUseCase = createProjectUseCase;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProjectRequest request) {
        Project project = createProjectUseCase.execute(new CreateProjectUseCase.CreateProjectCommand(userId(jwt), request.organizationId(), request.name(), request.description()));
        ProjectResponse response = ProjectResponse.from(project);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{projectId}").buildAndExpand(project.getId()).toUri()).body(response);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
