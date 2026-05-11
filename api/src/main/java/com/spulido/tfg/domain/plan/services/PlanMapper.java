package com.spulido.tfg.domain.plan.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.plan.model.Step;
import com.spulido.tfg.domain.plan.model.dto.PlanInfo;
import com.spulido.tfg.domain.plan.model.dto.PlanStepInfo;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.target.model.Target;

@Component
public class PlanMapper {

    private final TargetRepository targetRepository;

    public PlanMapper(TargetRepository targetRepository) {
        this.targetRepository = targetRepository;
    }

    public PlanInfo planToInfo(Plan plan) {
        return planToInfo(plan, null);
    }

    public PlanInfo planToInfo(Plan plan, String agentId) {
        if (plan == null) {
            return null;
        }

        String targetId = null;
        String targetIp = null;
        if (agentId != null) {
            Optional<Target> target = targetRepository.findByAssignedAgent(agentId);
            if (target.isPresent()) {
                targetId = target.get().getId();
                targetIp = target.get().getIpOrDomain();
            }
        }

        return PlanInfo.builder()
                .notes(plan.getNotes())
                .targetId(targetId)
                .targetIp(targetIp)
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
