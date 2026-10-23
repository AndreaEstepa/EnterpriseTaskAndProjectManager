package com.enterprise.taskmanager.domain.project;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;

import java.util.Locale;
import java.util.Objects;

public record ProjectName(String value) {
    public ProjectName{
        Objects.requireNonNull(value, "Project name must not be null");
        value = value.trim();
        int length = value.codePointCount(0, value.length());
        if (length < 2 || length > 100)
            throw new DomainValidationException("Project name must be between 2 and 100 characters");
    }

    public String normalized(){
        return value.toLowerCase(Locale.ROOT);
    }
}
