package com.spulido.agent.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.domain.task.TaskResult;

public class SshRemoteCommandExecutor implements RemoteCommandExecutor {

    private static final Logger log = LoggerFactory.getLogger(SshRemoteCommandExecutor.class);

    private final AgentConfig config;

    public SshRemoteCommandExecutor(AgentConfig config) {
        this.config = config;
    }

    @Override
    public TaskResult execute(TargetSession session, String command, long timeoutSeconds) {
        String taskId = "remote-" + System.currentTimeMillis();
        StringBuilder cmd = buildSshCommand(session, command);

        log.debug("Executing remote command on {}@{}", session.getTargetUser(), session.getTargetIp());

        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            String output;
            if (finished) {
                output = new String(process.getInputStream().readAllBytes());
            } else {
                process.destroyForcibly();
                String partial;
                try {
                    partial = new String(process.getInputStream().readAllBytes());
                } catch (IOException e) {
                    partial = "";
                }
                return TaskResult.failure(taskId, "Remote command timed out",
                        "Timed out after " + timeoutSeconds + "s. Partial output: " + partial);
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return TaskResult.success(taskId, output != null ? output.trim() : "");
            } else {
                return TaskResult.failure(taskId,
                        "Remote command failed with exit code " + exitCode,
                        "Exit: " + exitCode + ". Output: " + output);
            }
        } catch (Exception e) {
            return TaskResult.failure(taskId, "Remote command execution error",
                    e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    @Override
    public TaskResult transferFile(TargetSession session, byte[] content, String remotePath, String permissions) {
        String taskId = "transfer-" + System.currentTimeMillis();
        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("agent-transfer-", ".tmp");
            Files.write(tempFile, content);

            String userHost = session.getTargetUser() + "@" + session.getTargetIp();

            for (int scpAttempt = 1; scpAttempt <= 2; scpAttempt++) {
                try {
                    ProcessBuilder scpPb = new ProcessBuilder(
                            "scp", "-o", "StrictHostKeyChecking=no",
                            tempFile.toString(), userHost + ":" + remotePath);
                    scpPb.redirectErrorStream(true);
                    Process scpProcess = scpPb.start();
                    boolean scpFinished = scpProcess.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);

                    if (scpFinished && scpProcess.exitValue() == 0) {
                        log.info("SCP transfer succeeded to {}:{}", userHost, remotePath);
                        if (permissions != null && !permissions.isBlank()) {
                            execute(session, "chmod " + permissions + " " + remotePath, 10);
                        }
                        return TaskResult.success(taskId,
                                "File transferred via SCP to " + userHost + ":" + remotePath);
                    }

                    if (!scpFinished) {
                        scpProcess.destroyForcibly();
                    }
                    log.warn("SCP attempt {} failed (exit: {})",
                            scpAttempt, scpFinished ? scpProcess.exitValue() : "timeout");
                } catch (Exception e) {
                    log.warn("SCP attempt {} exception: {}", scpAttempt, e.getMessage());
                }
            }

            long maxSizeBytes = config.getExploitTransferFileMaxSizeMb() * 1024L * 1024L;
            if (content.length > maxSizeBytes) {
                return TaskResult.failure(taskId,
                        "Binary too large for pipe transfer",
                        "Binary size " + (content.length / 1024 / 1024) + "MB exceeds max "
                                + config.getExploitTransferFileMaxSizeMb() + "MB");
            }

            log.info("SCP failed after retries, falling back to base64 pipe transfer ({} bytes)", content.length);
            String base64Content = Base64.getEncoder().encodeToString(content);
            String pipeCommand = "echo '" + base64Content + "' | base64 -d > " + remotePath;

            TaskResult pipeResult = execute(session, pipeCommand, 120);
            if (pipeResult.isSuccess()) {
                if (permissions != null && !permissions.isBlank()) {
                    execute(session, "chmod " + permissions + " " + remotePath, 10);
                }
                log.info("Base64 pipe transfer succeeded to {}:{}", userHost, remotePath);
                return TaskResult.success(taskId,
                        "File transferred via base64 pipe to " + userHost + ":" + remotePath);
            }

            return TaskResult.failure(taskId, "All transfer methods failed",
                    "SCP and base64 pipe both failed. Pipe error: " + pipeResult.getFailureReason());

        } catch (IOException e) {
            return TaskResult.failure(taskId, "File transfer failed",
                    e.getMessage() != null ? e.getMessage() : "I/O error");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private StringBuilder buildSshCommand(TargetSession session, String command) {
        StringBuilder cmd = new StringBuilder("ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10");
        if (session.getSshIdentityFile() != null) {
            cmd.append(" -i ").append(session.getSshIdentityFile());
        }
        cmd.append(" ").append(session.getTargetUser()).append("@").append(session.getTargetIp());
        cmd.append(" '").append(command).append("'");
        return cmd;
    }
}
