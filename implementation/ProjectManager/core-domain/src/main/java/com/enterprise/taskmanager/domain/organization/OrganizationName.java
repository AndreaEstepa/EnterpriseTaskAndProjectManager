package com.enterprise.taskmanager.domain.organization;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;

import java.util.Locale;
import java.util.Objects;

public record OrganizationName(String value) {
    public OrganizationName {
        Objects.requireNonNull(value, "Organization name must not be null");
        value = value.trim();
        int length = value.codePointCount(0, value.length());
        if (length < 2 || length > 100) {
            throw new DomainValidationException("Organization name must be between 2 and 100 characters");
        }
    }

    public String normalized() {
        return value.toLowerCase(Locale.ROOT);
    }
}
