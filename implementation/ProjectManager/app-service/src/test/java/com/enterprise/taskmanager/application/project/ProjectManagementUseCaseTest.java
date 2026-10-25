package com.enterprise.taskmanager.application.project;

import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.shared.ProjectNameAlreadyTakenException;
import com.enterprise.taskmanager.application.user.UserRepository;
import com.enterprise.taskmanager.domain.organization.Organization;
import com.enterprise.taskmanager.domain.organization.OrganizationRole;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectName;
import com.enterprise.taskmanager.domain.project.ProjectStatus;
import com.enterprise.taskmanager.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectManagementUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    private final UserRepository users = mock(UserRepository.class);
    private final OrganizationQueryPort organizations = mock(OrganizationQueryPort.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final User user = User.register(USER_ID, "member@example.com", "Member User", "hash", NOW);
    private final Organization organization = Organization.create(ORGANIZATION_ID, "Acme", NOW);
    private final Project project = Project.create(PROJECT_ID, ORGANIZATION_ID, "My Project", null, NOW);

    @Test
    void listsProjectsOnlyForAnAccessibleOrganization() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID))
                .thenReturn(Optional.of(access(OrganizationRole.MEMBER)));
        when(projects.findAllByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(project));

        List<Project> result = new ListProjectsUseCase(users, organizations, projects)
                .execute(USER_ID, ORGANIZATION_ID);

        assertEquals(List.of(project), result);
        verify(projects).findAllByOrganizationId(ORGANIZATION_ID);
    }

    @Test
    void rejectsListingProjectsForAnInaccessibleOrganization() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> new ListProjectsUseCase(users, organizations, projects)
                        .execute(USER_ID, ORGANIZATION_ID));

        verify(projects, never()).findAllByOrganizationId(any());
    }

    @Test
    void updatesProjectForOrganizationMember() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID))
                .thenReturn(Optional.of(access(OrganizationRole.MEMBER)));
        when(projects.existsByOrganizationIdAndNameExceptProjectId(
                ORGANIZATION_ID, new ProjectName("Renamed Project"), PROJECT_ID)).thenReturn(false);
        when(projects.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project updated = new UpdateProjectUseCase(users, organizations, projects, clock).execute(
                new UpdateProjectUseCase.UpdateProjectCommand(
                        USER_ID, PROJECT_ID, "Renamed Project", "Updated description"));

        assertEquals("Renamed Project", updated.getName().value());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(NOW, updated.getUpdatedAt());
        verify(projects).save(updated);
    }

    @Test
    void rejectsDuplicateProjectNameWhenUpdating() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID))
                .thenReturn(Optional.of(access(OrganizationRole.MEMBER)));
        when(projects.existsByOrganizationIdAndNameExceptProjectId(
                ORGANIZATION_ID, new ProjectName("Existing Project"), PROJECT_ID)).thenReturn(true);

        assertThrows(
                ProjectNameAlreadyTakenException.class,
                () -> new UpdateProjectUseCase(users, organizations, projects, clock).execute(
                        new UpdateProjectUseCase.UpdateProjectCommand(
                                USER_ID, PROJECT_ID, "Existing Project", null)));

        verify(projects, never()).save(any(Project.class));
    }

    @Test
    void archivesProjectForAdmin() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID))
                .thenReturn(Optional.of(access(OrganizationRole.ADMIN)));
        when(projects.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project archived = new ArchiveProjectUseCase(users, organizations, projects, clock)
                .execute(PROJECT_ID, USER_ID);

        assertEquals(ProjectStatus.ARCHIVED, archived.getStatus());
        assertEquals(NOW, archived.getUpdatedAt());
        verify(projects).save(archived);
    }

    @Test
    void preventsMembersFromArchivingProjects() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID))
                .thenReturn(Optional.of(access(OrganizationRole.MEMBER)));

        assertThrows(
                AccessDeniedException.class,
                () -> new ArchiveProjectUseCase(users, organizations, projects, clock)
                        .execute(PROJECT_ID, USER_ID));

        verify(projects, never()).save(any(Project.class));
    }

    @Test
    void returnsProjectOnlyToOrganizationMembers() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID))
                .thenReturn(Optional.of(access(OrganizationRole.MEMBER)));

        Project found = new GetProjectUseCase(users, organizations, projects).execute(PROJECT_ID, USER_ID);

        assertEquals(PROJECT_ID, found.getId());
    }

    @Test
    void hidesProjectFromNonMembers() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                com.enterprise.taskmanager.application.shared.ResourceNotFoundException.class,
                () -> new GetProjectUseCase(users, organizations, projects).execute(PROJECT_ID, USER_ID));
    }

    private OrganizationQueryPort.OrganizationAccess access(OrganizationRole role) {
        return new OrganizationQueryPort.OrganizationAccess(organization, role);
    }
}
