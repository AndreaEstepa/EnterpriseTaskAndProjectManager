package com.enterprise.taskmanager.infrastructure.config;

import com.enterprise.taskmanager.application.organization.CreateOrganizationUseCase;
import com.enterprise.taskmanager.application.organization.GetOrganizationUseCase;
import com.enterprise.taskmanager.application.organization.ListOrganizationsUseCase;
import com.enterprise.taskmanager.application.organization.OrganizationProvisioningPort;
import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.project.CreateProjectUseCase;
import com.enterprise.taskmanager.application.project.ProjectRepository;
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
}
