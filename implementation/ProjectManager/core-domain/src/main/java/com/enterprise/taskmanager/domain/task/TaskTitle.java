package com.enterprise.taskmanager.domain.task;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;

import java.util.Objects;

public record TaskTitle(String value) {
    public TaskTitle {
        Objects.requireNonNull(value, "Task title must not be null");
        value = value.trim();
        int length = value.codePointCount(0, value.length());
        if (length < 2 || length > 150) {
            throw new DomainValidationException("Task title must be between 2 and 150 characters");
        }
    }
}
