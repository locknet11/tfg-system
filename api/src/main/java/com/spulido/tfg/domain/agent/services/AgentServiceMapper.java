package com.spulido.tfg.domain.agent.services;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.dto.AgentInfo;
import com.spulido.tfg.domain.agent.model.dto.AgentsList;
import com.spulido.tfg.domain.shared.ListMapper;
import com.spulido.tfg.domain.shared.ResponseList;
import com.spulido.tfg.domain.target.db.TargetRepository;

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
}
