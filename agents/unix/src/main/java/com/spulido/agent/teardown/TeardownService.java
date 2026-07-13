package com.spulido.agent.teardown;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.utils.AgentLifecycle;
import com.spulido.agent.worker.ScriptTemplateService;
import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.TeardownReportRequest;

/**
 * Orchestrates agent self-destruction: removes every artifact the agent placed
 * on the host, reports a per-artifact outcome to central for audit, then exits.
 *
 * <p>Guarantees: single-shot (concurrent triggers after the first are no-ops),
 * best-effort (each removal is independent and never aborts the rest),
 * idempotent (a missing artifact is {@code NOT_PRESENT}), and traceable (the
 * outcome is reported before local removal completes and the process exits).
 * The final report is flushed before the detached script unlinks the binary.
 */
@Service
public class TeardownService {

    private static final Logger log = LoggerFactory.getLogger(TeardownService.class);

    private static final String TEMPLATE_NAME = "self-destruct.sh.tmpl";

    /** Artifacts removed from Java (per-artifact outcome reported); the binary is left to the detached script. */
    private static final ArtifactType[] REPORTABLE_TYPES = {
            ArtifactType.AGENT_CONFIG,
            ArtifactType.AGENT_LOG,
            ArtifactType.RAW_DOWNLOAD,
            ArtifactType.INSTALL_SCRIPT,
            ArtifactType.DOWNLOADED_TOOLS,
            ArtifactType.WORKING_DIR,
            ArtifactType.OS_REGISTRATION
    };

    private final AgentConfig config;
    private final ArtifactSet artifactSet;
    private final ScriptTemplateService scriptTemplateService;
    private final DetachedCleanupLauncher cleanupLauncher;
    private final AgentLifecycle agentLifecycle;
    private final AgentHttpClient httpClient;

    private final AtomicBoolean tearingDown = new AtomicBoolean(false);

    public TeardownService(AgentConfig config,
                           ArtifactSet artifactSet,
                           ScriptTemplateService scriptTemplateService,
                           DetachedCleanupLauncher cleanupLauncher,
                           AgentLifecycle agentLifecycle,
                           AgentHttpClient httpClient) {
        this.config = config;
        this.artifactSet = artifactSet;
        this.scriptTemplateService = scriptTemplateService;
        this.cleanupLauncher = cleanupLauncher;
        this.agentLifecycle = agentLifecycle;
        this.httpClient = httpClient;
    }

    /**
     * True once teardown has started, so callers (worker loop) can stop accepting
     * new work.
     */
    public boolean isTearingDown() {
        return tearingDown.get();
    }

    /**
     * Tears the agent down for the given trigger. Runs at most once per instance.
     */
    public void selfDestruct(TeardownTrigger trigger) {
        if (!tearingDown.compareAndSet(false, true)) {
            log.info("Teardown already in progress; ignoring trigger {}", trigger);
            return;
        }
        log.info("Self-destruction initiated by trigger {}", trigger);

        Map<ArtifactType, Path> paths = new EnumMap<>(ArtifactType.class);
        try {
            paths.putAll(artifactSet.resolve());
        } catch (RuntimeException e) {
            log.warn("Failed to resolve artifact set: {}", e.getMessage());
        }

        List<ArtifactRemovalResult> results = removeReportableArtifacts(paths);

        reportOutcome(trigger, results);

        launchDetachedCleanup(paths);

        agentLifecycle.stop();
    }

    private List<ArtifactRemovalResult> removeReportableArtifacts(Map<ArtifactType, Path> paths) {
        List<ArtifactRemovalResult> results = new ArrayList<>();
        for (ArtifactType type : REPORTABLE_TYPES) {
            Path path = paths.get(type);
            RemovalStatus status;
            try {
                status = artifactSet.remove(path);
            } catch (RuntimeException e) {
                log.warn("Unexpected error removing {} ({}): {}", type, path, e.getMessage());
                status = RemovalStatus.FAILED;
            }
            results.add(new ArtifactRemovalResult(type, path != null ? path.toString() : null, status, ""));
        }
        return results;
    }

    private void reportOutcome(TeardownTrigger trigger, List<ArtifactRemovalResult> results) {
        try {
            List<TeardownReportRequest.ArtifactResult> dtoResults = new ArrayList<>();
            for (ArtifactRemovalResult r : results) {
                dtoResults.add(new TeardownReportRequest.ArtifactResult(
                        r.getType().name(), r.getPath(), r.getStatus().name(), r.getDetail()));
            }
            TeardownReportRequest request = new TeardownReportRequest(
                    config.getAgentId(), trigger.name(), Instant.now().toString(),
                    dtoResults, TeardownOutcome.BINARY_PENDING_DETACHED);
            httpClient.reportTeardownOutcome(request);
        } catch (RuntimeException e) {
            log.warn("Failed to report teardown outcome (continuing with teardown): {}", e.getMessage());
        }
    }

    private void launchDetachedCleanup(Map<ArtifactType, Path> paths) {
        try {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("AGENT_BINARY", pathString(paths.get(ArtifactType.AGENT_BINARY)));
            replacements.put("AGENT_PID", String.valueOf(ProcessHandle.current().pid()));
            replacements.put("CONFIG_FILE", pathString(paths.get(ArtifactType.AGENT_CONFIG)));
            replacements.put("LOG_FILE", pathString(paths.get(ArtifactType.AGENT_LOG)));
            replacements.put("RAW_DOWNLOAD", pathString(paths.get(ArtifactType.RAW_DOWNLOAD)));
            replacements.put("INSTALL_SCRIPT", pathString(paths.get(ArtifactType.INSTALL_SCRIPT)));
            replacements.put("TOOLS_DIR", pathString(paths.get(ArtifactType.DOWNLOADED_TOOLS)));
            replacements.put("WORKING_DIR", pathString(paths.get(ArtifactType.WORKING_DIR)));
            replacements.put("OS_REGISTRATION", pathString(paths.get(ArtifactType.OS_REGISTRATION)));

            String script = scriptTemplateService.renderTemplate(TEMPLATE_NAME, replacements);
            cleanupLauncher.launch(script);
        } catch (RuntimeException e) {
            log.warn("Failed to launch detached cleanup (continuing with shutdown): {}", e.getMessage());
        }
    }

    private String pathString(Path path) {
        return path != null ? path.toString() : "";
    }
}
