package com.enterprise.taskmanager.infrastructure.persistence.user;

import com.enterprise.taskmanager.application.shared.EmailAlreadyRegisteredException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.user.Email;
import com.enterprise.taskmanager.domain.user.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserPersistenceAdapter implements UserRepository {
    private final SpringDataUserRepository repository;
    public UserPersistenceAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEmail(Email email) {
        return repository.existsByNormalizedEmail(email.value());
    }

    @Override
    public User save(User user) {
        try {
            return repository.saveAndFlush(new UserJpaEntity(user)).toDomain();
        } catch (DataIntegrityViolationException exception) {
            String causeMessage = exception.getMostSpecificCause().getMessage();
            if (causeMessage != null && causeMessage.contains("uq_users_email_normalized")) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return repository.findByEmail(email.value()).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(UserJpaEntity::toDomain);
    }
}
