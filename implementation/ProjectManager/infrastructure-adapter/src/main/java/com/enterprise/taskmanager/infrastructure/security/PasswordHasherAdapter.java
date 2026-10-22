package com.enterprise.taskmanager.infrastructure.security;

import com.enterprise.taskmanager.application.user.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasherAdapter implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }
}
