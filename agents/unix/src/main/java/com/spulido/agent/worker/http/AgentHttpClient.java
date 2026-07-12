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
import com.spulido.agent.worker.http.dto.RegisterReplicatedRequest;
import com.spulido.agent.worker.http.dto.RegisterReplicatedResponse;
import com.spulido.agent.worker.http.dto.RemediationReportRequest;
import com.spulido.agent.worker.http.dto.RemediationReportResponse;
import com.spulido.agent.worker.http.dto.RemediationStrategyRequest;
import com.spulido.agent.worker.http.dto.RemediationStrategyResponse;
import com.spulido.agent.worker.http.dto.ReplicationRequestBody;
import com.spulido.agent.worker.http.dto.ReplicationRequestResponse;
import com.spulido.agent.worker.http.dto.ReplicationStatusResponse;
import com.spulido.agent.worker.http.dto.StepStatusResponse;
import com.spulido.agent.worker.http.dto.StepStatusUpdate;
import com.spulido.agent.worker.http.dto.VulnerabilityLookupRequest;
import com.spulido.agent.worker.http.dto.VulnerabilityLookupResponse;

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

    public VulnerabilityLookupResponse lookupVulnerabilities(VulnerabilityLookupRequest request) {
        String url = config.getCentralUrl() + "/api/agent/comm/vulnerabilities/lookup";
        log.info("Looking up vulnerabilities from central: {}", url);
        return restTemplate.postForObject(url, request, VulnerabilityLookupResponse.class);
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

    public RemediationStrategyResponse requestRemediationStrategy(RemediationStrategyRequest request) {
        String url = config.getCentralUrl() + "/api/agent/comm/remediation/strategy";
        log.info("Requesting remediation strategy from central: {}", url);
        return restTemplate.postForObject(url, request, RemediationStrategyResponse.class);
    }

    public RemediationReportResponse reportRemediationResult(RemediationReportRequest request) {
        String url = config.getCentralUrl() + "/api/agent/comm/remediation/report";
        log.info("Reporting remediation result to central: {}", url);
        return restTemplate.postForObject(url, request, RemediationReportResponse.class);
    }

    public ReplicationRequestResponse submitReplicationRequest(ReplicationRequestBody request) {
        String url = config.getCentralUrl() + "/api/agent/comm/replication-request";
        log.info("Submitting replication request to central: {}", url);
        return restTemplate.postForObject(url, request, ReplicationRequestResponse.class);
    }

    public ReplicationStatusResponse pollReplicationStatus(String requestId) {
        String url = config.getCentralUrl() + "/api/agent/comm/replication-request/" + requestId + "/status";
        log.info("Polling replication request status: {}", url);
        return restTemplate.getForObject(url, ReplicationStatusResponse.class);
    }

    public RegisterReplicatedResponse registerReplicated(String hostname, String os) {
        String url = config.getCentralUrl() + "/api/agent/replicated/register";
        log.info("Registering replicated agent with central: {}", url);
        RegisterReplicatedRequest body = new RegisterReplicatedRequest(config.getPreauthCode(), hostname, os);
        return restTemplate.postForObject(url, body, RegisterReplicatedResponse.class);
    }

    public byte[] downloadBinary(String downloadUrl) {
        log.info("Downloading agent binary from: {}", downloadUrl);
        return restTemplate.getForObject(downloadUrl, byte[].class);
    }
}
