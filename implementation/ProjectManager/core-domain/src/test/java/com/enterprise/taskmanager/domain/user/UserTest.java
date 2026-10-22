package com.enterprise.taskmanager.domain.user;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void registersActiveUserWithNormalizedEmailAndTrimmedName() {
        User user = User.register(USER_ID, "  Jane.Doe@Example.COM ", "  Jane Doe  ", "bcrypt-hash", CREATED_AT);

        assertEquals(USER_ID, user.id());
        assertEquals("jane.doe@example.com", user.email().value());
        assertEquals("Jane Doe", user.fullName());
        assertEquals("bcrypt-hash", user.passwordHash());
        assertEquals(UserStatus.ACTIVE, user.status());
        assertEquals(CREATED_AT, user.createdAt());
        assertEquals(CREATED_AT, user.updatedAt());
    }

    @Test
    void rejectsInvalidNameOrBlankPasswordHash() {
        assertThrows(
                DomainValidationException.class,
                () -> User.register(USER_ID, "jane@example.com", "J", "bcrypt-hash", CREATED_AT));
        assertThrows(
                DomainValidationException.class,
                () -> User.register(USER_ID, "jane@example.com", "Jane Doe", "  ", CREATED_AT));
    }

    @Test
    void profileUpdateReturnsUpdatedUserWithoutChangingCreationTime() {
        Instant updatedAt = CREATED_AT.plusSeconds(60);

        User updated = User.register(USER_ID, "jane@example.com", "Jane Doe", "bcrypt-hash", CREATED_AT)
                .updateFullName("  Jane Smith ", updatedAt);

        assertEquals("Jane Smith", updated.fullName());
        assertEquals(CREATED_AT, updated.createdAt());
        assertEquals(updatedAt, updated.updatedAt());
    }

    @Test
    void deactivatedUserCannotBeModified() {
        User inactive = User.register(USER_ID, "jane@example.com", "Jane Doe", "bcrypt-hash", CREATED_AT)
                .deactivate(CREATED_AT.plusSeconds(60));

        assertEquals(UserStatus.INACTIVE, inactive.status());
        assertThrows(
                DomainValidationException.class,
                () -> inactive.updateFullName("Jane Smith", CREATED_AT.plusSeconds(120)));
        assertThrows(
                DomainValidationException.class,
                () -> inactive.deactivate(CREATED_AT.plusSeconds(120)));
    }
}
