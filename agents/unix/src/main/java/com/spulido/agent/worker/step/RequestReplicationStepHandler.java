package com.spulido.agent.worker.step;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spulido.agent.domain.task.ServiceInfo;
import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.ExploitationKnowledgeResponse;
import com.spulido.agent.worker.http.dto.ReplicationRequestBody;
import com.spulido.agent.worker.http.dto.ReplicationRequestResponse;
import com.spulido.agent.worker.http.dto.ReplicationStatusResponse;

public class RequestReplicationStepHandler implements StepHandler {

    private static final Logger log = LoggerFactory.getLogger(RequestReplicationStepHandler.class);
    private static final int MAX_POLL_ATTEMPTS = 180;
    private static final long POLL_INTERVAL_MS = 10_000;
    private static final long MAX_BACKOFF_MS = 300_000;

    private final AgentHttpClient httpClient;

    public RequestReplicationStepHandler(AgentHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public StepResult handle(StepAction action, Map<StepAction, StepResult> context, String targetIp) {
        log.info("Handling REQUEST_REPLICATION step");

        StepResult exploitKnowledge = context.get(StepAction.EXPLOITATION_KNOWLEDGE);
        if (exploitKnowledge == null || exploitKnowledge.getScripts().isEmpty()) {
            log.warn("No exploit scripts available from EXPLOITATION_KNOWLEDGE step");
            return StepResult.success(action, List.of(), List.of(),
                    List.of("No exploit scripts available. Skipping replication request."));
        }

        StepResult serviceScan = context.get(StepAction.SERVICE_SCAN);
        if (serviceScan == null || serviceScan.getServices().isEmpty()) {
            log.warn("No service scan results available");
            return StepResult.failure(action, List.of("No service scan results available"));
        }

        ServiceInfo firstService = serviceScan.getServices().get(0);
        String script = exploitKnowledge.getScripts().get(0);
        String[] scriptParts = script.split("\\|");
        String exploitId = scriptParts.length > 0 ? scriptParts[0].trim() : "unknown";
        String cveId = scriptParts.length > 1 ? scriptParts[1].trim() : "unknown";
        String severity = scriptParts.length > 2 ? scriptParts[2].trim() : "HIGH";

        // Central resolves targetIp/targetPort per matched script (EXPLOITATION_KNOWLEDGE's own
        // logs) — SERVICE_SCAN's raw output has no such fields and, with multiple services
        // reported, "the first discovered service" is not reliably the one with an exploit.
        String resolvedTargetIp = extractFromLogs(exploitKnowledge.getLogs(), "targetIp:");
        String targetPort = extractFromLogs(exploitKnowledge.getLogs(), "targetPort:");

        ReplicationRequestBody request = new ReplicationRequestBody();
        request.setTargetIp(resolvedTargetIp != null ? resolvedTargetIp : "unknown");
        request.setTargetPort(targetPort != null ? Integer.parseInt(targetPort) : 0);
        request.setExploitId(exploitId);
        request.setCveId(cveId);
        request.setServiceName(firstService.getName());
        request.setServiceVersion(firstService.getVersion());
        request.setSeverity(severity);

        try {
            ReplicationRequestResponse response = httpClient.submitReplicationRequest(request);
            log.info("Replication request submitted: status={}", response.getStatus());

            if ("APPROVED".equals(response.getStatus())) {
                return StepResult.success(action, List.of(), List.of(),
                        List.of("replicationToken:" + response.getReplicationToken(),
                                "downloadUrl:" + response.getDownloadUrl(),
                                "preauthCode:" + response.getPreauthCode(),
                                "centralUrl:" + response.getCentralUrl()));
            }

            if ("DUPLICATE".equals(response.getStatus()) || "DENIED".equals(response.getStatus())) {
                return StepResult.success(action, List.of(), List.of(),
                        List.of("Replication request " + response.getStatus().toLowerCase() + ". Skipping."));
            }

            if ("PENDING".equals(response.getStatus())) {
                return pollForApproval(response.getId());
            }

            return StepResult.failure(action, List.of("Unexpected replication response status: " + response.getStatus()));

        } catch (Exception e) {
            log.error("Failed to submit replication request", e);
            return StepResult.failure(action, List.of("Failed to submit replication request: " + e.getMessage()));
        }
    }

    private StepResult pollForApproval(String requestId) {
        log.info("Polling for replication approval: requestId={}", requestId);
        long backoffMs = POLL_INTERVAL_MS;
        int attempts = 0;

        while (attempts < MAX_POLL_ATTEMPTS) {
            try {
                Thread.sleep(backoffMs);
                ReplicationStatusResponse status = httpClient.pollReplicationStatus(requestId);
                log.info("Poll result: status={}", status.getStatus());

                if ("APPROVED".equals(status.getStatus())) {
                    return StepResult.success(StepAction.REQUEST_REPLICATION, List.of(), List.of(),
                            List.of("replicationToken:" + status.getReplicationToken(),
                                    "downloadUrl:" + status.getDownloadUrl(),
                                    "preauthCode:" + status.getPreauthCode(),
                                    "centralUrl:" + status.getCentralUrl()));
                }

                if ("DENIED".equals(status.getStatus()) || "EXPIRED".equals(status.getStatus())) {
                    return StepResult.success(StepAction.REQUEST_REPLICATION, List.of(), List.of(),
                            List.of("Replication request " + status.getStatus().toLowerCase() + ". Skipping."));
                }

                backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
                attempts++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return StepResult.failure(StepAction.REQUEST_REPLICATION,
                        List.of("Polling interrupted"));
            } catch (Exception e) {
                log.error("Error polling replication status", e);
                backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
                attempts++;
            }
        }

        return StepResult.failure(StepAction.REQUEST_REPLICATION,
                List.of("Replication request polling timed out after " + MAX_POLL_ATTEMPTS + " attempts"));
    }

    private String extractFromLogs(List<String> logs, String prefix) {
        if (logs == null) return null;
        return logs.stream()
                .filter(l -> l.startsWith(prefix))
                .findFirst()
                .map(l -> l.substring(prefix.length()).trim())
                .orElse(null);
    }
}
