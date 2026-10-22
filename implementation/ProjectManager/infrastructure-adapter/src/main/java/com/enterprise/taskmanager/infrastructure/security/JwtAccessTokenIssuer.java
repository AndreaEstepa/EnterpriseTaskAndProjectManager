package com.enterprise.taskmanager.infrastructure.security;

import com.enterprise.taskmanager.application.user.AccessTokenIssuer;
import com.enterprise.taskmanager.domain.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public JwtAccessTokenIssuer(JwtEncoder jwtEncoder, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    @Override
    public IssuedAccessToken issueFor(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(TOKEN_LIFETIME);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("enterprise-task-manager")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.id().toString())
                .claim("email", user.email().value())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();
        return new IssuedAccessToken(token, expiresAt);
    }
}
