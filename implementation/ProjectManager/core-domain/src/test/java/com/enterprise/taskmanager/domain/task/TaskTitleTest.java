package com.enterprise.taskmanager.domain.task;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskTitleTest {

    @Test
    void trimsTaskTitle() {
        assertEquals("Fix login", new TaskTitle("  Fix login  ").value());
    }

    @Test
    void rejectsTitlesOutsideAllowedLength() {
        assertThrows(DomainValidationException.class, () -> new TaskTitle(" "));
        assertThrows(DomainValidationException.class, () -> new TaskTitle("A"));
        assertThrows(DomainValidationException.class, () -> new TaskTitle("T".repeat(151)));
    }
}
