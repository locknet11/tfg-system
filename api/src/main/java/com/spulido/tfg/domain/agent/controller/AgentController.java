package com.spulido.tfg.domain.agent.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.agent.services.AgentService;
import com.spulido.tfg.domain.agent.services.AgentServiceMapper;
import com.spulido.tfg.domain.agent.model.dto.AgentInfo;
import com.spulido.tfg.domain.shared.ResponseList;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AgentController {

    private final AgentService agentService;
    private final AgentServiceMapper mapper;

    @GetMapping()
    public ResponseEntity<ResponseList<AgentInfo>> getAgents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var agents = agentService.listAgents(PageRequest.of(page, size));
        return ResponseEntity.ok().body(mapper.agentsListToResponseList(agents));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAgent(@PathVariable("id") String id) {
        agentService.deleteAgent(id);
        return ResponseEntity.noContent().build();
    }
}
