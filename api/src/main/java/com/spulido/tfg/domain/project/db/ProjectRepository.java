package com.spulido.tfg.domain.project.db;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.spulido.tfg.domain.project.model.Project;
import com.spulido.tfg.domain.project.model.ProjectStatus;

public interface ProjectRepository extends MongoRepository<Project, String> {
    Optional<Project> findByName(String name);
    List<Project> findByOrganizationId(String organizationId);
    List<Project> findByMemberIdsContaining(String memberId);
    List<Project> findByOrganizationIdAndStatus(String organizationId, ProjectStatus status);
}