package com.spulido.tfg.domain.agent.services.impl.plan;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.services.AgentPlanService;
import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.plan.model.Step;
import com.spulido.tfg.domain.plan.model.StepExecutionStatus;
import com.spulido.tfg.domain.template.exception.TemplateException;
import com.spulido.tfg.domain.template.model.Template;
import com.spulido.tfg.domain.template.services.TemplateService;
import com.spulido.tfg.domain.template.services.TemplateServiceMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentPlanServiceImpl implements AgentPlanService {

    private final AgentRepository agentRepository;
    private final TemplateService templateService;
    private final TemplateServiceMapper templateMapper;

    @Override
    public Plan assignPlanFromTemplate(String agentId, String templateId) throws AgentException, TemplateException {
        Agent agent = getScopedAgent(agentId);
        Template template = templateService.getTemplate(templateId);
        Plan plan = templateMapper.templateToPlan(template);
        resetStepState(plan.getSteps());

        if (agent.getPlan() != null) {
            List<Plan> history = agent.getPlanHistory() != null ? agent.getPlanHistory() : new ArrayList<>();
            history.add(agent.getPlan());
            agent.setPlanHistory(history);
        }

        agent.setPlan(plan);
        agentRepository.save(agent);
        return plan;
    }

    @Override
    public Plan getAgentPlan(String agentId) throws AgentException {
        Agent agent = getScopedAgent(agentId);
        return agent.getPlan();
    }

    @Override
    public void clearAgentPlan(String agentId) throws AgentException {
        Agent agent = getScopedAgent(agentId);
        agent.setPlan(null);
        agentRepository.save(agent);
    }

    private Agent getScopedAgent(String agentId) throws AgentException {
        return agentRepository.findByIdScoped(agentId)
                .orElseThrow(() -> new AgentException("agent.error.notfound"));
    }

    private void resetStepState(List<Step> steps) {
        if (steps == null) {
            return;
        }
        steps.forEach(step -> {
            step.setStatus(StepExecutionStatus.PENDING);
            step.setLogs(null);
        });
    }
}
