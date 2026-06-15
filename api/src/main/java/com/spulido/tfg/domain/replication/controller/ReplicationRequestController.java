package com.spulido.tfg.domain.replication.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.replication.model.dto.CreateReplicationRequest;
import com.spulido.tfg.domain.replication.model.dto.ReplicationRequestResponse;
import com.spulido.tfg.domain.replication.model.dto.ReplicationStatusResponse;
import com.spulido.tfg.domain.replication.services.ReplicationRequestService;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.db.AgentRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT')")
public class ReplicationRequestController {

    private final ReplicationRequestService service;
    private final AgentRepository agentRepository;

    @PostMapping("/api/agent/comm/replication-request")
    public ResponseEntity<ReplicationRequestResponse> submitRequest(
            @Valid @RequestBody CreateReplicationRequest request,
            @AuthenticationPrincipal String agentId) {

        Agent agent = agentRepository.findById(agentId).orElse(null);
        String organizationId = agent != null ? agent.getOrganizationId() : "";
        String projectId = agent != null ? agent.getProjectId() : "";

        ReplicationRequestResponse response = service.createRequest(request, agentId, organizationId, projectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/agent/comm/replication-request/{id}/status")
    public ResponseEntity<ReplicationStatusResponse> getStatus(
            @PathVariable String id,
            @AuthenticationPrincipal String agentId) {

        ReplicationRequestResponse response = service.getRequestStatus(id, agentId);
        ReplicationStatusResponse statusResponse = ReplicationStatusResponse.builder()
                .status(response.getStatus())
                .replicationToken(response.getReplicationToken())
                .downloadUrl(response.getDownloadUrl())
                .preauthCode(response.getPreauthCode())
                .centralUrl(response.getCentralUrl())
                .build();
        return ResponseEntity.ok(statusResponse);
    }
}
