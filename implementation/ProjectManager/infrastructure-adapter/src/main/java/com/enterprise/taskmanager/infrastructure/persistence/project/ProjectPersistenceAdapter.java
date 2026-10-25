package com.enterprise.taskmanager.infrastructure.persistence.project;

import com.enterprise.taskmanager.application.project.ProjectRepository;
import com.enterprise.taskmanager.application.shared.ProjectNameAlreadyTakenException;
import com.enterprise.taskmanager.domain.project.Project;
import com.enterprise.taskmanager.domain.project.ProjectName;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ProjectPersistenceAdapter implements ProjectRepository {
    private final SpringDataProjectRepository projects;

    public ProjectPersistenceAdapter(SpringDataProjectRepository projects) {
        this.projects = projects;
    }

    @Override
    public boolean existsByOrganizationIdAndName(UUID organizationId, ProjectName name) {
        return projects.existsByOrganizationIdAndNormalizedName(organizationId, name.normalized());
    }

    @Override
    public Project save(Project project) {
        try{
            ProjectJpaEntity savedProject = projects.saveAndFlush(new ProjectJpaEntity(project));
            return savedProject.toDomain();
        }catch (DataIntegrityViolationException e){
            String msg = e.getMostSpecificCause().getMessage();
            if (msg != null && msg.contains("uq_projects_organization_name_normalized")){
                throw new ProjectNameAlreadyTakenException();
            }
            throw e;
        }
    }
}
