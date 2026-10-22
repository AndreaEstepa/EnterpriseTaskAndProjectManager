package com.enterprise.taskmanager.application.user;

import com.enterprise.taskmanager.domain.user.Email;
import com.enterprise.taskmanager.domain.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    boolean existsByEmail(Email email);
    User save(User user);
    Optional<User> findByEmail(Email email);
    Optional<User> findById(UUID id);
}
