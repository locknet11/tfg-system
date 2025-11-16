package com.spulido.tfg.domain.plan.model.dto;

import com.spulido.tfg.domain.plan.model.StepAction;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanStepRequest {

    @NotNull
    private StepAction action;
}