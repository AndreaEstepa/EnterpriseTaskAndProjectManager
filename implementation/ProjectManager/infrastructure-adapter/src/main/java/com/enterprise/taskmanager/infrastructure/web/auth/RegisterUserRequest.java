package com.enterprise.taskmanager.infrastructure.web.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 72) String password,
        @NotBlank @Size(min = 2, max = 100) String fullName) {
}
