package com.spulido.agent.worker.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.worker.http.dto.ExploitationKnowledgeRequest;
import com.spulido.agent.worker.http.dto.ExploitationKnowledgeResponse;
import com.spulido.agent.worker.http.dto.HeartbeatResponse;
import com.spulido.agent.worker.http.dto.PlanResponse;
import com.spulido.agent.worker.http.dto.StepStatusResponse;
import com.spulido.agent.worker.http.dto.StepStatusUpdate;

@Service
public class AgentHttpClient {

    private static final Logger log = LoggerFactory.getLogger(AgentHttpClient.class);

    private final RestTemplate restTemplate;
    private final AgentConfig config;

    public AgentHttpClient(RestTemplate restTemplate, AgentConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    public PlanResponse fetchPlan() {
        String url = config.getCentralUrl() + "/api/agent/comm/plan";
        log.info("Fetching plan from central: {}", url);
        return restTemplate.getForObject(url, PlanResponse.class);
    }

    public ExploitationKnowledgeResponse requestExploitationKnowledge(ExploitationKnowledgeRequest request) {
        String url = config.getCentralUrl() + "/api/agent/comm/exploitation-knowledge";
        log.info("Requesting exploitation knowledge from central: {}", url);
        return restTemplate.postForObject(url, request, ExploitationKnowledgeResponse.class);
    }

    public HeartbeatResponse sendHeartbeat() {
        String url = config.getCentralUrl() + "/api/agent/comm/heartbeat";
        log.info("Sending heartbeat to central: {}", url);
        return restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT, null,
                HeartbeatResponse.class).getBody();
    }

    public StepStatusResponse reportStepStatus(int stepIndex, String status, java.util.List<String> logs) {
        String url = config.getCentralUrl() + "/api/agent/comm/plan/step/" + stepIndex;
        log.info("Reporting step {} status to central: {}", stepIndex, status);
        StepStatusUpdate body = new StepStatusUpdate(status, logs);
        restTemplate.put(url, body);
        return new StepStatusResponse();
    }
}
