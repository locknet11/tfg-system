package com.spulido.tfg.domain.plan.model.dto;

import java.util.List;

import com.spulido.tfg.domain.plan.model.StepAction;
import com.spulido.tfg.domain.plan.model.StepExecutionStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlanStepInfo {

    private StepAction action;
    private StepExecutionStatus status;
    private List<String> logs;
}
