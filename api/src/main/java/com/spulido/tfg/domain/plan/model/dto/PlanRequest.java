package com.spulido.tfg.domain.plan.model.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanRequest {

    private String notes;

    private boolean allowTemplating;

    @NotEmpty
    private List<PlanStepRequest> steps;
}