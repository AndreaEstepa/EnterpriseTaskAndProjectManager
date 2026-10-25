package com.enterprise.taskmanager.infrastructure.persistence.organization;

import com.enterprise.taskmanager.application.organization.OrganizationProvisioningPort;
import com.enterprise.taskmanager.application.organization.OrganizationQueryPort.OrganizationAccess;
import com.enterprise.taskmanager.application.organization.OrganizationQueryPort;
import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.shared.OrganizationNameAlreadyTakenException;
import com.enterprise.taskmanager.domain.organization.Organization;
import com.enterprise.taskmanager.domain.organization.OrganizationMember;
import com.enterprise.taskmanager.domain.organization.OrganizationRole;
import com.enterprise.taskmanager.domain.user.User;
import com.enterprise.taskmanager.domain.user.UserStatus;
import com.enterprise.taskmanager.infrastructure.persistence.user.SpringDataUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrganizationPersistenceAdapter implements OrganizationProvisioningPort, OrganizationQueryPort {
    private final SpringDataOrganizationRepository organizations;
    private final SpringDataOrganizationMemberRepository members;
    private final SpringDataUserRepository users;

    public OrganizationPersistenceAdapter(SpringDataOrganizationRepository organizations, SpringDataOrganizationMemberRepository members, SpringDataUserRepository users) {
        this.organizations = organizations;
        this.members = members;
        this.users = users;
    }

    @Override
    @Transactional
    public Organization createWithAdmin(Organization organization, UUID adminUserId, Instant joinedAt) {
        User admin = users.findById(adminUserId)
                .map(user -> user.toDomain())
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .orElseThrow(AccessDeniedException::new);
        OrganizationMember membership = OrganizationMember.create(
                UUID.randomUUID(), organization, admin, OrganizationRole.ADMIN, joinedAt);

        try {
            OrganizationJpaEntity savedOrganization = organizations.saveAndFlush(new OrganizationJpaEntity(organization));
            members.saveAndFlush(new OrganizationMemberJpaEntity(membership));
            return savedOrganization.toDomain();
        } catch (DataIntegrityViolationException exception) {
            String causeMessage = exception.getMostSpecificCause().getMessage();
            if (causeMessage != null && causeMessage.contains("uq_organizations_name_normalized")) {
                throw new OrganizationNameAlreadyTakenException();
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationAccess> findAllForMember(UUID userId) {
        return organizations.findAllForMember(userId).stream()
                .map(this::toAccess)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrganizationAccess> findAccessibleById(UUID organizationId, UUID userId) {
        return organizations.findAccessibleById(organizationId, userId).map(this::toAccess);
    }

    private OrganizationAccess toAccess(OrganizationAccessProjection projection) {
        Organization organization = Organization.restore(projection.getOrganizationId(), projection.getOrganizationName(), projection.getCreatedAt(), projection.getUpdatedAt());
        return new OrganizationAccess(organization, OrganizationRole.valueOf(projection.getRole()));
    }
}
