package com.enterprise.taskmanager.domain.user;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class User {

    private final UUID id;
    private final Email email;
    private final String fullName;
    private final String passwordHash;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(
            UUID id,
            Email email,
            String fullName,
            String passwordHash,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "User id must not be null");
        this.email = Objects.requireNonNull(email, "Email must not be null");
        this.fullName = validateFullName(fullName);
        this.passwordHash = validatePasswordHash(passwordHash);
        this.status = Objects.requireNonNull(status, "User status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at must not be null");

        if (updatedAt.isBefore(createdAt)) {
            throw new DomainValidationException("Updated at must not be before created at");
        }
    }

    public static User register(UUID id, String email, String fullName, String passwordHash, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        return new User(id, new Email(email), fullName, passwordHash, UserStatus.ACTIVE, now, now);
    }

    public static User restore(
            UUID id,
            String email,
            String fullName,
            String passwordHash,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return new User(id, new Email(email), fullName, passwordHash, status, createdAt, updatedAt);
    }

    public User updateFullName(String newFullName, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        ensureActive();
        if (now.isBefore(updatedAt)) {
            throw new DomainValidationException("Updated at must not be before the previous update");
        }
        return new User(id, email, newFullName, passwordHash, status, createdAt, now);
    }

    public User deactivate(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        ensureActive();
        if (now.isBefore(updatedAt)) {
            throw new DomainValidationException("Updated at must not be before the previous update");
        }
        return new User(id, email, fullName, passwordHash, UserStatus.INACTIVE, createdAt, now);
    }

    private void ensureActive() {
        if (status != UserStatus.ACTIVE) {
            throw new DomainValidationException("Inactive users cannot be modified");
        }
    }

    private static String validateFullName(String fullName) {
        Objects.requireNonNull(fullName, "Full name must not be null");
        String normalized = fullName.trim();
        int length = normalized.codePointCount(0, normalized.length());
        if (length < 2 || length > 100) {
            throw new DomainValidationException("Full name must be between 2 and 100 characters");
        }
        return normalized;
    }

    private static String validatePasswordHash(String passwordHash) {
        Objects.requireNonNull(passwordHash, "Password hash must not be null");
        if (passwordHash.isBlank()) {
            throw new DomainValidationException("Password hash must not be blank");
        }
        return passwordHash;
    }

    public UUID id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public UserStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
