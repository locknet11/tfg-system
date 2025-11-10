package com.spulido.tfg.domain.agent.model.dto;

import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.agent.model.Agent;

public class AgentsList extends PageImpl<Agent> {

    public AgentsList(List<Agent> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }
}
