package com.spulido.tfg.domain.agent.model.dto;

import com.spulido.tfg.domain.plan.model.dto.PlanRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignPlanRequest {

    @NotNull(message = "useTemplate is required")
    private Boolean useTemplate;

    private String templateId;

    @Valid
    private PlanRequest plan;

    // Validation handled in service
}