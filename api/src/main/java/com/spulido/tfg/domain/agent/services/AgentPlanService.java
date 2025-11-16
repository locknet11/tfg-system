package com.spulido.tfg.domain.agent.services;

import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.template.exception.TemplateException;

public interface AgentPlanService {

    Plan assignPlanFromTemplate(String agentId, String templateId) throws AgentException, TemplateException;

    Plan getAgentPlan(String agentId) throws AgentException;

    void clearAgentPlan(String agentId) throws AgentException;
}
