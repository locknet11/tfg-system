package com.spulido.tfg.domain.project.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {
    @NotNull
    String name;

    String description;

    @NotNull
    String organizationId;
}