package com.enterprise.taskmanager.infrastructure.web.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(@NotBlank @Size(min = 2, max = 100) String name) {
}
