package com.enterprise.taskmanager.domain.project;

import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectTest {
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void createsActiveProjectWithTrimmedValues() {
        Project project = Project.create(PROJECT_ID, ORGANIZATION_ID, "  My Project  ", "  Project goals  ", CREATED_AT);
        assertEquals(PROJECT_ID, project.getId());
        assertEquals(ORGANIZATION_ID, project.getOrganizationId());
        assertEquals("My Project", project.getName().value());
        assertEquals("Project goals", project.getDescription());
        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
        assertEquals(CREATED_AT, project.getCreatedAt());
        assertEquals(CREATED_AT, project.getUpdatedAt());
    }

    @Test
    void permitsMissingDescription() {
        Project project = Project.create(PROJECT_ID, ORGANIZATION_ID, "My Project", null, CREATED_AT);
        assertNull(project.getDescription());
    }

    @Test
    void renamesUpdatesDescriptionAndArchivesWithoutChangingIdentity() {
        Project project = Project.create(PROJECT_ID, ORGANIZATION_ID, "My Project", null, CREATED_AT);
        Instant renamedAt = CREATED_AT.plusSeconds(60);
        Project renamed = project.rename("Renamed Project", renamedAt);
        Instant descriptionUpdatedAt = renamedAt.plusSeconds(60);
        Project described = renamed.updateDescription("Project goals", descriptionUpdatedAt);
        Instant archivedAt = descriptionUpdatedAt.plusSeconds(60);

        Project archived = described.archive(archivedAt);
        assertEquals(PROJECT_ID, archived.getId());
        assertEquals(ORGANIZATION_ID, archived.getOrganizationId());
        assertEquals("Renamed Project", archived.getName().value());
        assertEquals("Project goals", archived.getDescription());
        assertEquals(ProjectStatus.ARCHIVED, archived.getStatus());
        assertEquals(CREATED_AT, archived.getCreatedAt());
        assertEquals(archivedAt, archived.getUpdatedAt());
    }

    @Test
    void rejectsModificationsToArchivedProject() {
        Project archived = Project.restore(PROJECT_ID, ORGANIZATION_ID, "My Project", null, ProjectStatus.ARCHIVED, CREATED_AT, CREATED_AT.plusSeconds(60));
        Instant later = CREATED_AT.plusSeconds(120);

        assertThrows(DomainValidationException.class, () -> archived.rename("Another Project", later));
        assertThrows(DomainValidationException.class, () -> archived.updateDescription("New description", later));
        assertThrows(DomainValidationException.class, () -> archived.archive(later));
    }

    @Test
    void rejectsModificationWithEarlierTimestamp() {
        Project project = Project.create(PROJECT_ID, ORGANIZATION_ID, "My Project", null, CREATED_AT);
        assertThrows(DomainValidationException.class, () -> project.rename("Another Project", CREATED_AT.minusSeconds(1)));
    }

    @Test
    void restoresArchivedStatusAndRejectsInvalidDescription() {
        assertEquals(ProjectStatus.ARCHIVED, Project.restore(PROJECT_ID, ORGANIZATION_ID, "My Project", null, ProjectStatus.ARCHIVED, CREATED_AT, CREATED_AT).getStatus());
        assertThrows(DomainValidationException.class, () -> Project.create(PROJECT_ID, ORGANIZATION_ID, "My Project", "D".repeat(501), CREATED_AT));
    }
}
