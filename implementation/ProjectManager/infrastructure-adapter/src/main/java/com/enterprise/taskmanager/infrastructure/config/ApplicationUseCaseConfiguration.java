package com.enterprise.taskmanager.infrastructure.config;

import com.enterprise.taskmanager.application.organization.CreateOrganizationUseCase;
import com.enterprise.taskmanager.application.organization.GetOrganizationUseCase;
import com.enterprise.taskmanager.application.organization.ListOrganizationsUseCase;
import com.enterprise.taskmanager.application.organization.OrganizationProvisioningPort;
import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.project.ArchiveProjectUseCase;
import com.enterprise.taskmanager.application.project.CreateProjectUseCase;
import com.enterprise.taskmanager.application.project.GetProjectUseCase;
import com.enterprise.taskmanager.application.project.ListProjectsUseCase;
import com.enterprise.taskmanager.application.project.ProjectRepository;
import com.enterprise.taskmanager.application.project.UpdateProjectUseCase;
import com.enterprise.taskmanager.application.task.AssignTaskUseCase;
import com.enterprise.taskmanager.application.task.CreateTaskUseCase;
import com.enterprise.taskmanager.application.task.GetTaskUseCase;
import com.enterprise.taskmanager.application.task.ListTasksUseCase;
import com.enterprise.taskmanager.application.task.TaskRepository;
import com.enterprise.taskmanager.application.task.TransitionTaskStatusUseCase;
import com.enterprise.taskmanager.application.task.UpdateTaskUseCase;
import com.enterprise.taskmanager.application.user.AccessTokenIssuer;
import com.enterprise.taskmanager.application.user.AuthenticateUserUseCase;
import com.enterprise.taskmanager.application.user.PasswordHasher;
import com.enterprise.taskmanager.application.user.RegisterUserUseCase;
import com.enterprise.taskmanager.application.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationUseCaseConfiguration {

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepository users, PasswordHasher passwordHasher, Clock applicationClock) {
        return new RegisterUserUseCase(users, passwordHasher, applicationClock);
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(UserRepository users, PasswordHasher passwordHasher, AccessTokenIssuer accessTokenIssuer) {
        return new AuthenticateUserUseCase(users, passwordHasher, accessTokenIssuer);
    }

    @Bean
    CreateOrganizationUseCase createOrganizationUseCase(UserRepository users, OrganizationProvisioningPort provisioningPort, Clock applicationClock) {
        return new CreateOrganizationUseCase(users, provisioningPort, applicationClock);
    }

    @Bean
    ListOrganizationsUseCase listOrganizationsUseCase(UserRepository users, OrganizationQueryPort organizationQueryPort) {
        return new ListOrganizationsUseCase(users, organizationQueryPort);
    }

    @Bean
    GetOrganizationUseCase getOrganizationUseCase(UserRepository users, OrganizationQueryPort organizationQueryPort) {
        return new GetOrganizationUseCase(users, organizationQueryPort);
    }

    @Bean
    CreateProjectUseCase createProjectUseCase(UserRepository users, OrganizationQueryPort organizationQueryPort, ProjectRepository projects, Clock applicationClock){
        return new CreateProjectUseCase(users, organizationQueryPort, projects, applicationClock);
    }

    @Bean
    ListProjectsUseCase listProjectsUseCase(
            UserRepository users,
            OrganizationQueryPort organizationQueryPort,
            ProjectRepository projects) {
        return new ListProjectsUseCase(users, organizationQueryPort, projects);
    }

    @Bean
    GetProjectUseCase getProjectUseCase(
            UserRepository users,
            OrganizationQueryPort organizationQueryPort,
            ProjectRepository projects) {
        return new GetProjectUseCase(users, organizationQueryPort, projects);
    }

    @Bean
    UpdateProjectUseCase updateProjectUseCase(
            UserRepository users,
            OrganizationQueryPort organizationQueryPort,
            ProjectRepository projects,
            Clock applicationClock) {
        return new UpdateProjectUseCase(users, organizationQueryPort, projects, applicationClock);
    }

    @Bean
    ArchiveProjectUseCase archiveProjectUseCase(
            UserRepository users,
            OrganizationQueryPort organizationQueryPort,
            ProjectRepository projects,
            Clock applicationClock) {
        return new ArchiveProjectUseCase(users, organizationQueryPort, projects, applicationClock);
    }

    @Bean
    CreateTaskUseCase createTaskUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks,
            Clock applicationClock) {
        return new CreateTaskUseCase(users, projects, organizations, tasks, applicationClock);
    }

    @Bean
    ListTasksUseCase listTasksUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks) {
        return new ListTasksUseCase(users, projects, organizations, tasks);
    }

    @Bean
    GetTaskUseCase getTaskUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks) {
        return new GetTaskUseCase(users, projects, organizations, tasks);
    }

    @Bean
    UpdateTaskUseCase updateTaskUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks,
            Clock applicationClock) {
        return new UpdateTaskUseCase(users, projects, organizations, tasks, applicationClock);
    }

    @Bean
    AssignTaskUseCase assignTaskUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks,
            Clock applicationClock) {
        return new AssignTaskUseCase(users, projects, organizations, tasks, applicationClock);
    }

    @Bean
    TransitionTaskStatusUseCase transitionTaskStatusUseCase(
            UserRepository users,
            ProjectRepository projects,
            OrganizationQueryPort organizations,
            TaskRepository tasks,
            Clock applicationClock) {
        return new TransitionTaskStatusUseCase(users, projects, organizations, tasks, applicationClock);
    }
}
