package com.spulido.tfg.domain.plan.model.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlanInfo {

    private String notes;
    private boolean allowTemplating;
    private List<PlanStepInfo> steps;
}
