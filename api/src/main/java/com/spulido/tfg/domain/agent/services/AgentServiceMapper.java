package com.spulido.tfg.domain.agent.services;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.dto.AgentInfo;
import com.spulido.tfg.domain.agent.model.dto.AgentRegistrationResponse;
import com.spulido.tfg.domain.agent.model.dto.AgentsList;
import com.spulido.tfg.domain.agent.model.dto.RegisterAgentRequest;
import com.spulido.tfg.domain.shared.ListMapper;
import com.spulido.tfg.domain.shared.ResponseList;
import com.spulido.tfg.domain.target.db.TargetRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class AgentServiceMapper {

    private final ModelMapper modelMapper;
    private final TargetRepository targetRepository;

    public AgentInfo agentToAgentInfo(Agent agent) {
        AgentInfo info = modelMapper.map(agent, AgentInfo.class);
        targetRepository.findByAssignedAgent(agent.getId())
                .ifPresent(target -> info.setTargetSystem(target.getSystemName()));
        return info;
    }

    public ResponseList<AgentInfo> agentsListToResponseList(AgentsList list) {
        List<AgentInfo> infoList = agentListToInfoList(list.getContent());
        PageImpl<AgentInfo> dtoPage = new PageImpl<>(infoList, list.getPageable(), list.getTotalElements());
        return ListMapper.mapList(dtoPage);
    }

    private List<AgentInfo> agentListToInfoList(List<Agent> agents) {
        return agents.stream().map(this::agentToAgentInfo).collect(Collectors.toList());
    }

    public RegisterAgentRequest pathVariablesToRegisterRequest(
            String organizationIdentifier,
            String projectIdentifier,
            String targetUniqueId,
            HttpServletRequest httpRequest) {
        RegisterAgentRequest request = new RegisterAgentRequest();
        request.setOrganizationIdentifier(organizationIdentifier);
        request.setProjectIdentifier(projectIdentifier);
        request.setTargetUniqueId(targetUniqueId);
        request.setClientIp(extractClientIp(httpRequest));
        return request;
    }

    public AgentRegistrationResponse toRegistrationResponse(AgentRegistrationResponse response) {
        return modelMapper.map(response, AgentRegistrationResponse.class);
    }

    /**
     * Extracts the client IP address from the HTTP request.
     * Checks common proxy headers first, then falls back to remote address.
     */
    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }
}
