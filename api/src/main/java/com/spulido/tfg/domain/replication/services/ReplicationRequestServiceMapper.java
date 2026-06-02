package com.spulido.tfg.domain.replication.services;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.spulido.tfg.domain.shared.ResponseList;
import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.replication.model.ReplicationRequest;
import com.spulido.tfg.domain.replication.model.dto.ReplicationRequestInfo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReplicationRequestServiceMapper {

    private final AgentRepository agentRepository;

    public ReplicationRequestInfo toInfo(ReplicationRequest request) {
        ReplicationRequestInfo info = new ReplicationRequestInfo();
        info.setId(request.getId());
        info.setParentAgentId(request.getParentAgentId());
        info.setTargetIp(request.getTargetIp());
        info.setTargetPort(request.getTargetPort());
        info.setExploitId(request.getExploitId());
        info.setCveId(request.getCveId());
        info.setServiceName(request.getServiceName());
        info.setServiceVersion(request.getServiceVersion());
        info.setSeverity(request.getSeverity());
        info.setStatus(request.getStatus());
        info.setPolicy(request.getPolicy());
        info.setApprovedBy(request.getApprovedBy());
        info.setCreatedAt(request.getCreatedAt());
        info.setExpiresAt(request.getExpiresAt());
        info.setResolvedAt(request.getResolvedAt());

        Agent parentAgent = agentRepository.findById(request.getParentAgentId()).orElse(null);
        if (parentAgent != null) {
            info.setParentAgentName(parentAgent.getName());
        }

        return info;
    }

    public ResponseList<ReplicationRequestInfo> toResponseList(Page<ReplicationRequest> page) {
        Page<ReplicationRequestInfo> infoPage = page.map(this::toInfo);
        ResponseList<ReplicationRequestInfo> response = new ResponseList<>();
        response.setContent(infoPage.getContent());
        response.setTotalElements(infoPage.getTotalElements());
        response.setTotalPages(infoPage.getTotalPages());
        response.setPage(infoPage.getNumber());
        response.setSize(infoPage.getSize());
        return response;
    }
}
