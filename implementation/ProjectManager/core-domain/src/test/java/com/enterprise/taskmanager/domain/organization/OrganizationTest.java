package com.enterprise.taskmanager.domain.organization;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void createsOrganizationAndRenamesWithoutChangingCreationTime() {
        Organization organization = Organization.create(ORGANIZATION_ID, "Acme Corporation", CREATED_AT);
        Instant updatedAt = CREATED_AT.plusSeconds(60);

        Organization renamed = organization.rename("Acme Global", updatedAt);

        assertEquals(ORGANIZATION_ID, renamed.id());
        assertEquals("Acme Global", renamed.name().value());
        assertEquals(CREATED_AT, renamed.createdAt());
        assertEquals(updatedAt, renamed.updatedAt());
    }

    @Test
    void rejectsRenameWithInvalidNameOrEarlierTimestamp() {
        Organization organization = Organization.create(ORGANIZATION_ID, "Acme Corporation", CREATED_AT);

        assertThrows(DomainValidationException.class, () -> organization.rename(" ", CREATED_AT.plusSeconds(1)));
        assertThrows(DomainValidationException.class, () -> organization.rename("Acme Global", CREATED_AT.minusSeconds(1)));
    }
}
