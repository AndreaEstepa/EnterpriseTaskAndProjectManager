package com.enterprise.taskmanager.infrastructure.web.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProjectRequest(@NotNull UUID organizationId, @NotBlank @Size(min = 2, max = 100) String name, @Size(min=0, max = 500) String description) {
}
