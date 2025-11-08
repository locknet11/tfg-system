package com.spulido.tfg.domain.organization.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrganizationRequest {
    @NotNull
    String name;

    String description;
}