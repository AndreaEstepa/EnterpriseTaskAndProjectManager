package com.enterprise.taskmanager.domain.project;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectNameTest {

    @Test
    void trimsNameAndNormalizesItForComparison() {
        ProjectName name = new ProjectName("  My Project  ");
        assertEquals("My Project", name.value());
        assertEquals("my project", name.normalized());
    }

    @Test
    void rejectsNamesOutsideAllowedLength() {
        assertThrows(DomainValidationException.class, () -> new ProjectName(" "));
        assertThrows(DomainValidationException.class, () -> new ProjectName("A"));
        assertThrows(DomainValidationException.class, () -> new ProjectName("A".repeat(101)));
    }
}
