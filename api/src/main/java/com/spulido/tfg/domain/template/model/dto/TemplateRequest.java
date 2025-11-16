package com.spulido.tfg.domain.template.model.dto;

import com.spulido.tfg.domain.plan.model.dto.PlanRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private PlanRequest plan;
}
