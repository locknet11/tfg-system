package com.spulido.tfg.domain.agent.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.dto.AgentInfo;
import com.spulido.tfg.domain.agent.model.dto.AgentRegistrationResponse;
import com.spulido.tfg.domain.agent.model.dto.AgentsList;
import com.spulido.tfg.domain.agent.model.dto.RegisterAgentRequest;
import com.spulido.tfg.domain.agent.services.AgentService;
import com.spulido.tfg.domain.agent.services.AgentServiceMapper;
import com.spulido.tfg.domain.shared.ResponseList;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AgentServiceMapper mapper;

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

    @PostMapping("/{organizationIdentifier}/{projectIdentifier}/{targetUniqueId}")
    public ResponseEntity<?> registerAgent(
            @PathVariable("organizationIdentifier") String organizationIdentifier,
            @PathVariable("projectIdentifier") String projectIdentifier,
            @PathVariable("targetUniqueId") String targetUniqueId,
            HttpServletRequest httpRequest) throws AgentException {

        RegisterAgentRequest request = mapper.pathVariablesToRegisterRequest(
                organizationIdentifier,
                projectIdentifier,
                targetUniqueId,
                httpRequest);

        AgentRegistrationResponse response = agentService.registerAgent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
