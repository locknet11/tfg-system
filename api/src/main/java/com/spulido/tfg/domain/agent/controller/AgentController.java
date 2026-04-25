package com.spulido.tfg.domain.agent.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.dto.AgentInfo;
import com.spulido.tfg.domain.agent.model.dto.AgentRegistrationResponse;
import com.spulido.tfg.domain.agent.model.dto.AgentsList;
import com.spulido.tfg.domain.agent.model.dto.AssignPlanRequest;
import com.spulido.tfg.domain.agent.model.dto.RegisterAgentRequest;
import com.spulido.tfg.domain.agent.services.AgentPlanService;
import com.spulido.tfg.domain.agent.services.AgentService;
import com.spulido.tfg.domain.agent.services.AgentServiceMapper;
import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.plan.model.dto.PlanInfo;
import com.spulido.tfg.domain.plan.services.PlanMapper;
import com.spulido.tfg.domain.script.services.ScriptService;
import com.spulido.tfg.domain.shared.ResponseList;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.services.TargetService;
import com.spulido.tfg.domain.template.exception.TemplateException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final TargetService targetService;
    private final AgentServiceMapper mapper;
    private final AgentPlanService agentPlanService;
    private final PlanMapper planMapper;
    private final ScriptService scriptService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseList<AgentInfo>> getAgents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        AgentsList agents = agentService.listAgents(PageRequest.of(page, size));
        return ResponseEntity.ok().body(mapper.agentsListToResponseList(agents));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteAgent(@PathVariable("id") String id) {
        agentService.deleteAgent(id);
        return ResponseEntity.noContent().build();
    }

    // public method to register an agent
    @PostMapping(value = "/{organizationIdentifier}/{projectIdentifier}/{targetUniqueId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> registerAgent(
            @PathVariable String organizationIdentifier,
            @PathVariable String projectIdentifier,
            @PathVariable String targetUniqueId,
            @RequestParam(name = "preauthCode", required = false) String preauthCode,
            HttpServletRequest httpRequest) {

        RegisterAgentRequest request = mapper.pathVariablesToRegisterRequest(
                organizationIdentifier,
                projectIdentifier,
                targetUniqueId,
                preauthCode,
                httpRequest);

        try {
            AgentRegistrationResponse response = agentService.registerAgent(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(response.getInstallScript());
        } catch (AgentException e) {
            // get script from classpath
            Target target = null;

            try {
                target = targetService.getByUniqueId(targetUniqueId);
            } catch (Exception ex) {
                Map<String, String> errorDetails = new HashMap<>();
                errorDetails.put("error", e.getMessage());
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorDetails.toString());
            }

            String errorScript = scriptService.generateInstallErrorScript(
                    target.getOs(),
                    targetUniqueId,
                    e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(errorScript);
        }
    }

    @PutMapping("/{id}/plan")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgentInfo> assignPlan(
            @PathVariable("id") String id,
            @RequestBody AssignPlanRequest request) throws AgentException {
        var agent = agentService.assignPlan(id, request);
        return ResponseEntity.ok(mapper.agentToAgentInfo(agent));
    }

    @GetMapping("/{id}/plan")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlanInfo> getPlan(@PathVariable("id") String id) throws AgentException {
        Plan plan = agentPlanService.getAgentPlan(id);
        if (plan == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(planMapper.planToInfo(plan));
    }

    @PostMapping("/{id}/plan/from-template/{templateId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlanInfo> assignPlanFromTemplate(
            @PathVariable("id") String id,
            @PathVariable("templateId") String templateId) throws AgentException, TemplateException {
        Plan plan = agentPlanService.assignPlanFromTemplate(id, templateId);
        return ResponseEntity.ok(planMapper.planToInfo(plan));
    }

    @DeleteMapping("/{id}/plan")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearPlan(@PathVariable("id") String id) throws AgentException {
        agentPlanService.clearAgentPlan(id);
        return ResponseEntity.noContent().build();
    }
}
