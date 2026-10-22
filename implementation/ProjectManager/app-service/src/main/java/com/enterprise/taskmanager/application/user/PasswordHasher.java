package com.enterprise.taskmanager.application.user;

public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String passwordHash);
}
