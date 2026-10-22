package com.enterprise.taskmanager.domain.organization;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import com.enterprise.taskmanager.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationMemberTest {

    private static final Instant JOINED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void createsMembershipWithRequestedRoleAndEntityIds() {
        Organization organization = Organization.create(UUID.randomUUID(), "Acme Corporation", JOINED_AT);
        User user = User.register(UUID.randomUUID(), "jane@example.com", "Jane Doe", "bcrypt-hash", JOINED_AT);
        UUID membershipId = UUID.randomUUID();

        OrganizationMember member = OrganizationMember.create(
                membershipId, organization, user, OrganizationRole.ADMIN, JOINED_AT);

        assertEquals(membershipId, member.id());
        assertEquals(organization.id(), member.organizationId());
        assertEquals(user.id(), member.userId());
        assertEquals(OrganizationRole.ADMIN, member.role());
        assertEquals(JOINED_AT, member.joinedAt());
    }

    @Test
    void rejectsInactiveUserMembership() {
        Organization organization = Organization.create(UUID.randomUUID(), "Acme Corporation", JOINED_AT);
        User inactiveUser = User.register(
                UUID.randomUUID(), "jane@example.com", "Jane Doe", "bcrypt-hash", JOINED_AT)
                .deactivate(JOINED_AT.plusSeconds(1));

        assertThrows(
                DomainValidationException.class,
                () -> OrganizationMember.create(
                        UUID.randomUUID(), organization, inactiveUser, OrganizationRole.MEMBER, JOINED_AT));
    }

    @Test
    void rejectsNullRole() {
        Organization organization = Organization.create(UUID.randomUUID(), "Acme Corporation", JOINED_AT);
        User user = User.register(UUID.randomUUID(), "jane@example.com", "Jane Doe", "bcrypt-hash", JOINED_AT);

        assertThrows(
                NullPointerException.class,
                () -> OrganizationMember.create(UUID.randomUUID(), organization, user, null, JOINED_AT));
    }
}
