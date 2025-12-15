package com.spulido.tfg.domain.agent.model.dto;

import java.util.List;

import com.spulido.tfg.domain.plan.model.StepExecutionStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStepRequest {

    @NotNull
    private StepExecutionStatus status;

    private List<String> logs;
}
