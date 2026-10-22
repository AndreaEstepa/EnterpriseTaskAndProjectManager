package com.enterprise.taskmanager.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {
    @Query(value = "SELECT EXISTS (SELECT 1 FROM users WHERE lower(btrim(email)) = :email)", nativeQuery = true)
    boolean existsByNormalizedEmail(@Param("email") String email);
    Optional<UserJpaEntity> findByEmail(String email);
}
