package com.spulido.tfg.domain.target.model.dto;

import com.spulido.tfg.domain.target.model.OperatingSystem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTargetRequest {

    @NotBlank(message = "System name is required")
    private String systemName;

    private String description;

    @NotNull(message = "Operating system is required")
    private OperatingSystem os;

    @NotBlank(message = "Project ID is required")
    private String projectId;

}