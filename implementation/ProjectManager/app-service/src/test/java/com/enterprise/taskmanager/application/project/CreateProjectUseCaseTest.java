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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateProjectUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private final UserRepository users = mock(UserRepository.class);
    private final OrganizationQueryPort organizations = mock(OrganizationQueryPort.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final CreateProjectUseCase useCase = new CreateProjectUseCase(users, organizations, projects, clock);

    @Test
    void createsActiveProjectForOrganizationMember() {
        User user = User.register(USER_ID, "pepe@example.com", "Pepe Doe", "bcrypt-hash", NOW);
        Organization organization = Organization.create(ORGANIZATION_ID, "Acme", NOW);
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID)).thenReturn(Optional.of(new OrganizationQueryPort.OrganizationAccess(organization, OrganizationRole.MEMBER)));
        when(projects.existsByOrganizationIdAndName(ORGANIZATION_ID, new ProjectName("Acme Website"))).thenReturn(false);
        when(projects.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project created = useCase.execute(new CreateProjectUseCase.CreateProjectCommand(USER_ID, ORGANIZATION_ID, "Acme Website", "Website redesign"));
        assertEquals(ORGANIZATION_ID, created.getOrganizationId());
        assertEquals("Acme Website", created.getName().value());
        assertEquals("Website redesign", created.getDescription());
        assertEquals(ProjectStatus.ACTIVE, created.getStatus());
        assertEquals(NOW, created.getCreatedAt());
        verify(projects).save(created);
    }

    @Test
    void rejectsUserWhoIsNotOrganizationMember() {
        User user = User.register(USER_ID, "pepe@example.com", "Pepe Doe", "bcrypt-hash", NOW);
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> useCase.execute(new CreateProjectUseCase.CreateProjectCommand(USER_ID, ORGANIZATION_ID, "Acme Website", null)));
        verify(projects, never()).save(any(Project.class));
    }

    @Test
    void rejectsDuplicateProjectNameWithinOrganization() {
        User user = User.register(USER_ID, "pepe@example.com", "Pepe Doe", "bcrypt-hash", NOW);
        Organization organization = Organization.create(ORGANIZATION_ID, "Acme", NOW);
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(organizations.findAccessibleById(ORGANIZATION_ID, USER_ID)).thenReturn(Optional.of(new OrganizationQueryPort.OrganizationAccess(organization, OrganizationRole.MEMBER)));
        when(projects.existsByOrganizationIdAndName(ORGANIZATION_ID, new ProjectName("Acme Website"))).thenReturn(true);
        assertThrows(ProjectNameAlreadyTakenException.class, () -> useCase.execute(new CreateProjectUseCase.CreateProjectCommand(USER_ID, ORGANIZATION_ID, "Acme Website", null)));
        verify(projects, never()).save(any(Project.class));
    }
}
