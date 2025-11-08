package com.spulido.tfg.domain.project.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.organization.exception.OrganizationException;
import com.spulido.tfg.domain.organization.services.OrganizationService;
import com.spulido.tfg.domain.project.exception.ProjectException;
import com.spulido.tfg.domain.project.model.Project;
import com.spulido.tfg.domain.project.model.dto.CreateProjectRequest;
import com.spulido.tfg.domain.project.model.dto.ProjectInfo;
import com.spulido.tfg.domain.project.model.dto.UpdateProjectRequest;
import com.spulido.tfg.domain.project.model.dto.UpdateProjectStatusRequest;
import com.spulido.tfg.domain.project.services.ProjectService;
import com.spulido.tfg.domain.project.services.ProjectServiceMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProjectController {

    private final ProjectService projectService;
    private final OrganizationService organizationService;
    private final ProjectServiceMapper projectMapper;

    @GetMapping
    public ResponseEntity<List<ProjectInfo>> getProjects(@RequestParam(required = false) String organizationId) {
        List<Project> projects;
        if (organizationId != null) {
            // Check if organization exists
            try {
                organizationService.getById(organizationId);
                projects = projectService.getProjectsByOrganization(organizationId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            // Return all projects since there's only one admin user
            projects = projectService.getAllProjects();
        }

        List<ProjectInfo> projectInfos = projectMapper.projectsToProjectInfos(projects);
        return ResponseEntity.ok().body(projectInfos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectInfo> getProjectById(@PathVariable("id") String id) throws ProjectException {
        Project project = projectService.getById(id);
        ProjectInfo projectInfo = projectMapper.projectToProjectInfo(project);
        return ResponseEntity.ok().body(projectInfo);
    }

    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody @Valid CreateProjectRequest request)
            throws URISyntaxException, ProjectException {
        // Verify organization exists
        try {
            organizationService.getById(request.getOrganizationId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        Project project = projectMapper.createProjectRequestToProject(request);

        Project created = projectService.createProject(project);

        // Add project to organization
        try {
            organizationService.addProject(request.getOrganizationId(), created.getId());
        } catch (OrganizationException e) {
            // Rollback project creation if organization update fails
            try {
                projectService.deleteProject(created.getId());
            } catch (ProjectException rollbackException) {
                // Log rollback failure but continue
            }
            throw new ProjectException("Failed to associate project with organization: " + e.getMessage());
        }

        ProjectInfo projectInfo = projectMapper.projectToProjectInfo(created);
        return ResponseEntity.created(new URI(String.format("/api/projects/%s", created.getId()))).body(projectInfo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectInfo> updateProject(@PathVariable("id") String id,
            @RequestBody @Valid UpdateProjectRequest request) throws ProjectException {
        Project project = projectService.getById(id);
        Project updatedProject = projectMapper.updateProjectRequestToProject(request, project);
        Project updated = projectService.updateProject(updatedProject);
        ProjectInfo projectInfo = projectMapper.projectToProjectInfo(updated);
        return ResponseEntity.ok().body(projectInfo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable("id") String id) throws ProjectException {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateProjectStatus(@PathVariable("id") String id,
            @RequestBody UpdateProjectStatusRequest request) throws ProjectException {
        projectService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }
}