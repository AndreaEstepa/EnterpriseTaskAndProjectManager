package com.enterprise.taskmanager.application.user;

import com.enterprise.taskmanager.domain.user.User;

import java.time.Instant;

public interface AccessTokenIssuer {
    IssuedAccessToken issueFor(User user);
    record IssuedAccessToken(String value, Instant expiresAt) {
    }
}
