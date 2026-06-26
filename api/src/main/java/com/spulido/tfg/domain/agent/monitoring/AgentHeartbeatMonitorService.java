package com.spulido.tfg.domain.agent.monitoring;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.AgentStatus;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.model.TargetStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentHeartbeatMonitorService {

    private static final List<AgentStatus> CANDIDATE_STATUSES = Arrays.asList(
            AgentStatus.ACTIVE, AgentStatus.CREATED);

    private final AgentRepository agentRepository;
    private final TargetRepository targetRepository;
    private final HeartbeatConfig heartbeatConfig;

    @Scheduled(fixedDelayString = "${heartbeat.scheduler.delay-ms:30000}")
    public void evaluateAgentHeartbeats() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(heartbeatConfig.getTimeoutSeconds());
        List<Agent> staleAgents = agentRepository.findByStatusInAndLastConnectionBefore(
                CANDIDATE_STATUSES, cutoff);

        if (staleAgents.isEmpty()) {
            return;
        }

        log.info("Heartbeat monitor: found {} stale agents (cutoff: {})", staleAgents.size(), cutoff);

        for (Agent agent : staleAgents) {
            agent.setStatus(AgentStatus.UNRESPONSIVE);
            agentRepository.save(agent);

            targetRepository.findByAssignedAgent(agent.getId()).ifPresent(target -> {
                if (target.getStatus() == TargetStatus.ONLINE) {
                    target.setStatus(TargetStatus.OFFLINE);
                    targetRepository.save(target);
                    log.info("Target {} marked OFFLINE (agent {} heartbeat stale)", target.getId(), agent.getName());
                }
            });

            log.info("Agent {} marked UNRESPONSIVE (last heartbeat: {})", agent.getName(), agent.getLastConnection());
        }
    }
}
