package com.spulido.tfg.domain.agent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.services.AgentPlanService;
import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.plan.model.dto.PlanInfo;
import com.spulido.tfg.domain.plan.services.PlanMapper;
import com.spulido.tfg.domain.template.exception.TemplateException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agent/{agentId}/plan")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AgentPlanController {

    private final AgentPlanService agentPlanService;
    private final PlanMapper planMapper;

    @GetMapping
    public ResponseEntity<PlanInfo> getPlan(@PathVariable("agentId") String agentId) throws AgentException {
        Plan plan = agentPlanService.getAgentPlan(agentId);
        if (plan == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(planMapper.planToInfo(plan));
    }

    @PostMapping("/from-template/{templateId}")
    public ResponseEntity<PlanInfo> assignPlanFromTemplate(
            @PathVariable("agentId") String agentId,
            @PathVariable("templateId") String templateId) throws AgentException, TemplateException {
        Plan plan = agentPlanService.assignPlanFromTemplate(agentId, templateId);
        return ResponseEntity.ok(planMapper.planToInfo(plan));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearPlan(@PathVariable("agentId") String agentId) throws AgentException {
        agentPlanService.clearAgentPlan(agentId);
        return ResponseEntity.noContent().build();
    }
}
