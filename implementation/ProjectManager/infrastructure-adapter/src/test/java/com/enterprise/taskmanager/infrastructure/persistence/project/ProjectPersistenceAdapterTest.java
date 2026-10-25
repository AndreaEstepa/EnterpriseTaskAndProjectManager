package com.enterprise.taskmanager.infrastructure.persistence.project;

import com.enterprise.taskmanager.application.shared.ProjectNameAlreadyTakenException;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectName;
import com.enterprise.taskmanager.domain.project.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectPersistenceAdapterTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-10-25T10:00:00Z");

    private final SpringDataProjectRepository repository = mock(SpringDataProjectRepository.class);
    private final ProjectPersistenceAdapter adapter = new ProjectPersistenceAdapter(repository);

    @Test
    void checksProjectNameUsingOrganizationAndNormalizedName() {
        ProjectName name = new ProjectName("  My Project  ");
        when(repository.existsByOrganizationIdAndNormalizedName(ORGANIZATION_ID, "my project"))
                .thenReturn(true);

        boolean exists = adapter.existsByOrganizationIdAndName(ORGANIZATION_ID, name);

        assertTrue(exists);
        verify(repository).existsByOrganizationIdAndNormalizedName(ORGANIZATION_ID, "my project");
    }

    @Test
    void savesProjectAndMapsSavedEntityBackToDomain() {
        Project project = Project.create(
                PROJECT_ID, ORGANIZATION_ID, "My Project", "Project description", CREATED_AT);
        when(repository.saveAndFlush(any(ProjectJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project saved = adapter.save(project);

        assertEquals(project.getId(), saved.getId());
        assertEquals(project.getOrganizationId(), saved.getOrganizationId());
        assertEquals(project.getName(), saved.getName());
        assertEquals(project.getDescription(), saved.getDescription());
        assertEquals(ProjectStatus.ACTIVE, saved.getStatus());
        assertEquals(project.getCreatedAt(), saved.getCreatedAt());
        assertEquals(project.getUpdatedAt(), saved.getUpdatedAt());
        verify(repository).saveAndFlush(any(ProjectJpaEntity.class));
    }

    @Test
    void translatesDuplicateProjectNameConstraintViolation() {
        Project project = Project.create(
                PROJECT_ID, ORGANIZATION_ID, "My Project", null, CREATED_AT);
        DataIntegrityViolationException persistenceException = new DataIntegrityViolationException(
                "Could not save project",
                new SQLException("duplicate key violates uq_projects_organization_name_normalized"));
        when(repository.saveAndFlush(any(ProjectJpaEntity.class))).thenThrow(persistenceException);

        assertThrows(ProjectNameAlreadyTakenException.class, () -> adapter.save(project));
    }

    @Test
    void rethrowsUnrelatedIntegrityViolations() {
        Project project = Project.create(
                PROJECT_ID, ORGANIZATION_ID, "My Project", null, CREATED_AT);
        DataIntegrityViolationException persistenceException = new DataIntegrityViolationException(
                "Could not save project",
                new SQLException("foreign key constraint violation"));
        when(repository.saveAndFlush(any(ProjectJpaEntity.class))).thenThrow(persistenceException);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class, () -> adapter.save(project));

        assertEquals(persistenceException, thrown);
    }
}
