package com.spulido.tfg.domain.project.services.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

import com.spulido.tfg.common.util.IdentifierGenerator;
import com.spulido.tfg.domain.project.db.ProjectRepository;
import com.spulido.tfg.domain.project.exception.ProjectException;
import com.spulido.tfg.domain.project.model.Project;
import com.spulido.tfg.domain.project.model.ProjectStatus;
import com.spulido.tfg.domain.project.services.ProjectService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository repository;

    @Override
    public Project createProject(Project project) throws ProjectException {
        if (projectNameExists(project.getName())) {
            throw new ProjectException("project.name.alreadyExists");
        }
        
        // Generate unique project identifier
        String identifier;
        do {
            identifier = IdentifierGenerator.generateProjectIdentifier();
        } while (repository.findByProjectIdentifier(identifier).isPresent());
        
        project.setProjectIdentifier(identifier);
        return repository.save(project);
    }

    @Override
    public Project updateProject(Project project) throws ProjectException {
        if (!repository.existsById(project.getId())) {
            throw new ProjectException("project.error.notfound");
        }
        return repository.save(project);
    }

    @Override
    public Project getById(String id) throws ProjectException {
        return repository.findById(id).orElseThrow(() -> new ProjectException("project.error.notfound"));
    }

    @Override
    public Project getByName(String name) throws ProjectException {
        return repository.findByName(name).orElseThrow(() -> new ProjectException("project.error.notfound"));
    }

    @Override
    public List<Project> getProjectsByOrganization(String organizationId) {
        return repository.findByOrganizationId(organizationId);
    }

    @Override
    public List<Project> getProjectsByOrganizationAndStatus(String organizationId, ProjectStatus status) {
        return repository.findByOrganizationIdAndStatus(organizationId, status);
    }

    @Override
    public List<Project> getAllProjects() {
        return repository.findAll();
    }

    @Override
    public void deleteProject(String projectId) throws ProjectException {
        if (!repository.existsById(projectId)) {
            throw new ProjectException("project.error.notfound");
        }
        repository.deleteById(projectId);
    }

    @Override
    public boolean projectNameExists(String name) {
        Optional<Project> projectOptional = repository.findByName(name);
        return projectOptional.isPresent();
    }

    @Override
    public void updateStatus(String projectId, ProjectStatus status) throws ProjectException {
        Project project = getById(projectId);
        project.setStatus(status);
        repository.save(project);
    }

    @Override
    public Project getByProjectIdentifier(String projectIdentifier) throws ProjectException {
        return repository.findByProjectIdentifier(projectIdentifier)
                .orElseThrow(() -> new ProjectException("project.error.notfound"));
    }
}