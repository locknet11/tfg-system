package com.spulido.tfg.domain.project.model.dto;

import com.spulido.tfg.domain.project.model.ProjectStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectStatusRequest {
    @NotNull
    ProjectStatus status;
}