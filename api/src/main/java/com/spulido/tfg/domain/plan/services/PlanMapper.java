package com.spulido.tfg.domain.plan.services;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.plan.model.Step;
import com.spulido.tfg.domain.plan.model.dto.PlanInfo;
import com.spulido.tfg.domain.plan.model.dto.PlanStepInfo;

@Component
public class PlanMapper {

    public PlanInfo planToInfo(Plan plan) {
        if (plan == null) {
            return null;
        }
        return PlanInfo.builder()
                .notes(plan.getNotes())
                .steps(buildStepInfos(plan.getSteps()))
                .build();
    }

    private List<PlanStepInfo> buildStepInfos(List<Step> steps) {
        if (steps == null) {
            return Collections.emptyList();
        }
        return steps.stream()
                .map(step -> PlanStepInfo.builder()
                        .action(step.getAction())
                        .status(step.getStatus())
                        .logs(step.getLogs())
                        .build())
                .collect(Collectors.toList());
    }
}
