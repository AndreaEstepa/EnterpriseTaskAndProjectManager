package com.enterprise.taskmanager.infrastructure.persistence.user;

import com.enterprise.taskmanager.domain.user.User;
import com.enterprise.taskmanager.domain.user.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 255)
    private String email;
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(User user) {
        id = user.id();
        email = user.email().value();
        fullName = user.fullName();
        passwordHash = user.passwordHash();
        status = user.status();
        createdAt = user.createdAt();
        updatedAt = user.updatedAt();
    }

    public User toDomain() {
        return User.restore(id, email, fullName, passwordHash, status, createdAt, updatedAt);
    }
}
