package com.spulido.tfg.domain.project.services;

import com.spulido.tfg.domain.project.model.Project;
import com.spulido.tfg.domain.project.model.dto.CreateProjectRequest;
import com.spulido.tfg.domain.project.model.dto.ProjectInfo;
import com.spulido.tfg.domain.project.model.dto.UpdateProjectRequest;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProjectServiceMapper {

    private final ModelMapper modelMapper;

    public ProjectServiceMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Project createProjectRequestToProject(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName())
               .setDescription(request.getDescription())
               .setOrganizationId(request.getOrganizationId())
               .setCreatedAt(LocalDateTime.now());
        return project;
    }

    public Project updateProjectRequestToProject(UpdateProjectRequest request, Project project) {
        project.setName(request.getName())
               .setDescription(request.getDescription())
               .setUpdatedAt(LocalDateTime.now());
        return project;
    }

    public ProjectInfo projectToProjectInfo(Project project) {
        return modelMapper.map(project, ProjectInfo.class);
    }

    public List<ProjectInfo> projectsToProjectInfos(List<Project> projects) {
        return projects.stream()
                .map(this::projectToProjectInfo)
                .collect(Collectors.toList());
    }
}