package com.enterprise.taskmanager.infrastructure.web.auth;

import com.enterprise.taskmanager.domain.user.User;
import com.enterprise.taskmanager.domain.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id(),
                user.email().value(),
                user.fullName(),
                user.status(),
                user.createdAt(),
                user.updatedAt());
    }
}
