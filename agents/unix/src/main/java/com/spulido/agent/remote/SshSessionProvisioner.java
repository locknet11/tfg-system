package com.spulido.agent.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spulido.agent.config.AgentConfig;

public class SshSessionProvisioner {

    private static final Logger log = LoggerFactory.getLogger(SshSessionProvisioner.class);

    private final AgentConfig config;

    public SshSessionProvisioner(AgentConfig config) {
        this.config = config;
    }

    public boolean verify(String targetIp, String targetUser, String identityFile) {
        String user = (targetUser != null && !targetUser.isBlank())
                ? targetUser : config.getExploitDefaultTargetUser();

        for (int attempt = 1; attempt <= 3; attempt++) {
            log.info("SSH connectivity probe attempt {}/3 to {}@{}", attempt, user, targetIp);
            try {
                StringBuilder cmd = new StringBuilder("ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5");
                if (identityFile != null && !identityFile.isBlank()) {
                    cmd.append(" -i ").append(identityFile);
                }
                cmd.append(" ").append(user).append("@").append(targetIp);
                cmd.append(" 'echo OK'");

                ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd.toString());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    String output = new String(process.getInputStream().readAllBytes());
                    if (output.contains("OK")) {
                        log.info("SSH connectivity confirmed to {}@{}", user, targetIp);
                        return true;
                    }
                } else {
                    if (!finished) {
                        process.destroyForcibly();
                    }
                }
            } catch (Exception e) {
                log.warn("SSH probe attempt {} failed: {}", attempt, e.getMessage());
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

        log.error("SSH connectivity verification failed after 3 attempts to {}@{}", user, targetIp);
        return false;
    }
}
