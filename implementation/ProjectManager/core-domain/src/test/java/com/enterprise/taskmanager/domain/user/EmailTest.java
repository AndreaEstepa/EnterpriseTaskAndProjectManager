package com.enterprise.taskmanager.domain.user;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

    @Test
    void trimsAndLowercasesAddress() {
        Email email = new Email("  Jane.Doe@Example.COM ");

        assertEquals("jane.doe@example.com", email.value());
    }

    @Test
    void rejectsMalformedAddress() {
        assertThrows(DomainValidationException.class, () -> new Email("not-an-email"));
    }

    @Test
    void rejectsAddressLongerThan255Characters() {
        String localPart = "a".repeat(244);
        String email = localPart + "@example.com";

        assertThrows(DomainValidationException.class, () -> new Email(email));
    }
}
