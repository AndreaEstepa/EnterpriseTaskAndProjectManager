package com.enterprise.taskmanager.domain.organization;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationNameTest {

    @Test
    void trimsDisplayNameAndProvidesCaseInsensitiveUniquenessKey() {
        OrganizationName name = new OrganizationName("  Acme Corporation  ");

        assertEquals("Acme Corporation", name.value());
        assertEquals("acme corporation", name.normalized());
        assertEquals(name.normalized(), new OrganizationName("ACME CORPORATION").normalized());
    }

    @Test
    void rejectsNamesOutsideAllowedLength() {
        assertThrows(DomainValidationException.class, () -> new OrganizationName(" "));
        assertThrows(DomainValidationException.class, () -> new OrganizationName("A"));
        assertThrows(DomainValidationException.class, () -> new OrganizationName("A".repeat(101)));
    }
}
