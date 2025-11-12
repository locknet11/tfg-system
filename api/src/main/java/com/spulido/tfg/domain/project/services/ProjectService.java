package com.spulido.tfg.domain.project.services;

import java.util.List;

import com.spulido.tfg.domain.project.exception.ProjectException;
import com.spulido.tfg.domain.project.model.Project;
import com.spulido.tfg.domain.project.model.ProjectStatus;

public interface ProjectService {
    Project createProject(Project project) throws ProjectException;

    Project updateProject(Project project) throws ProjectException;

    Project getById(String id) throws ProjectException;

    Project getByName(String name) throws ProjectException;

    List<Project> getProjectsByOrganization(String organizationId);

    List<Project> getProjectsByOrganizationAndStatus(String organizationId, ProjectStatus status);

    List<Project> getAllProjects();

    void deleteProject(String projectId) throws ProjectException;

    boolean projectNameExists(String name);

    void updateStatus(String projectId, ProjectStatus status) throws ProjectException;

    Project getByProjectIdentifier(String projectIdentifier) throws ProjectException;
}