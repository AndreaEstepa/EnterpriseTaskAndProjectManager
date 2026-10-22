package com.enterprise.taskmanager.domain.user;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final int MAX_LENGTH = 255;
    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s.]+(?:\\.[^@\\s.]+)+$");

    public Email {
        Objects.requireNonNull(value, "Email must not be null");
        value = value.trim().toLowerCase(Locale.ROOT);

        int length = value.codePointCount(0, value.length());
        if (length == 0 || length > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new DomainValidationException("Email must be a valid address of at most 255 characters");
        }
    }
}
