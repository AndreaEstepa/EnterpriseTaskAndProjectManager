package com.enterprise.taskmanager.infrastructure.web.organization;

import com.enterprise.taskmanager.application.organization.CreateOrganizationUseCase;
import com.enterprise.taskmanager.application.organization.GetOrganizationUseCase;
import com.enterprise.taskmanager.application.organization.ListOrganizationsUseCase;
import com.enterprise.taskmanager.domain.organization.Organization;
import com.enterprise.taskmanager.domain.organization.OrganizationRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {
    private final CreateOrganizationUseCase createOrganization;
    private final ListOrganizationsUseCase listOrganizations;
    private final GetOrganizationUseCase getOrganization;

    public OrganizationController(CreateOrganizationUseCase createOrganization, ListOrganizationsUseCase listOrganizations, GetOrganizationUseCase getOrganization) {
        this.createOrganization = createOrganization;
        this.listOrganizations = listOrganizations;
        this.getOrganization = getOrganization;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateOrganizationRequest request) {
        Organization organization = createOrganization.execute(new CreateOrganizationUseCase.CreateOrganizationCommand(userId(jwt), request.name()));
        OrganizationResponse response = OrganizationResponse.from(organization, OrganizationRole.ADMIN);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{organizationId}").buildAndExpand(organization.id()).toUri()).body(response);
    }

    @GetMapping
    public List<OrganizationResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return listOrganizations.execute(userId(jwt)).stream().map(OrganizationResponse::from).toList();
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId) {
        return OrganizationResponse.from(getOrganization.execute(organizationId, userId(jwt)));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
