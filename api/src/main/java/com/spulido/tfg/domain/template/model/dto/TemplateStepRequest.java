package com.spulido.tfg.domain.template.model.dto;

import com.spulido.tfg.domain.plan.model.StepAction;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateStepRequest {

    @NotNull
    private StepAction action;
}
