package com.spulido.agent.worker.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spulido.agent.container.ContainerDetectionResult;
import com.spulido.agent.container.ContainerDetector;
import com.spulido.agent.container.DetectionConfidence;
import com.spulido.agent.domain.task.ServiceInfo;
import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.worker.CommandExecutor;
import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.RemediationReportRequest;
import com.spulido.agent.worker.http.dto.RemediationStrategyRequest;
import com.spulido.agent.worker.http.dto.RemediationStrategyResponse;
import com.spulido.agent.worker.http.dto.VulnerabilityLookupRequest;
import com.spulido.agent.worker.http.dto.VulnerabilityLookupResponse;

/**
 * Step handler for REMEDIATE action.
 * Executes remediation commands on the target system based on strategies from the central platform.
 * Before remediation, checks whether the agent is running inside a container
 * and skips with a documented reason if so.
 */
public class RemediationStepHandler implements StepHandler {

    private static final Logger log = LoggerFactory.getLogger(RemediationStepHandler.class);
    private static final String KERNEL_PACKAGE_PATTERN = "^linux-(image|headers|modules)-.*";
    private static final long COMMAND_TIMEOUT_SECONDS = 300;

    private final AgentHttpClient httpClient;
    private final CommandExecutor commandExecutor;
    private final ContainerDetector containerDetector;

    public RemediationStepHandler(AgentHttpClient httpClient, CommandExecutor commandExecutor) {
        this.httpClient = httpClient;
        this.commandExecutor = commandExecutor;
        this.containerDetector = new ContainerDetector();
    }

    public RemediationStepHandler(AgentHttpClient httpClient, CommandExecutor commandExecutor,
                                   ContainerDetector containerDetector) {
        this.httpClient = httpClient;
        this.commandExecutor = commandExecutor;
        this.containerDetector = containerDetector;
    }

    @Override
    public StepResult handle(StepAction action, Map<StepAction, StepResult> context, String targetIp) {
        log.info("Starting remediation step handler");

        // Pre-condition: detect container runtime and skip remediation if found
        ContainerDetectionResult detection = containerDetector.detect();
        if (detection.isContainer()) {
            log.info("Container runtime detected ({}): skipping all remediation actions",
                    detection.getRuntimeName() != null ? detection.getRuntimeName() : "unknown");
            return handleContainerSkip(action, detection, context);
        }
        log.info("No container detected — proceeding with remediation");

        StepResult serviceScanResult = context.get(StepAction.SERVICE_SCAN);
        if (serviceScanResult == null || serviceScanResult.getServices().isEmpty()) {
            log.warn("No service scan data found in context, skipping remediation");
            return StepResult.success(action, List.of(), List.of(),
                    List.of("No services to remediate"));
        }

        List<ServiceInfo> services = serviceScanResult.getServices();
        String targetId = extractFromLogs(serviceScanResult.getLogs(), "targetId:");
        String operatingSystem = detectOperatingSystem();

        log.info("Found {} services to check for vulnerabilities, targetId: {}, OS: {}",
                services.size(), targetId, operatingSystem);

        List<String> allLogs = new ArrayList<>();
        int totalCves = 0;
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        int pendingRebootCount = 0;

        for (ServiceInfo service : services) {
            log.info("Checking vulnerabilities for service: {} version {}",
                    service.getName(), service.getVersion());

            try {
                VulnerabilityLookupRequest vulnRequest = new VulnerabilityLookupRequest(
                        service.getName(), service.getVersion());
                VulnerabilityLookupResponse vulnResponse = httpClient.lookupVulnerabilities(vulnRequest);

                if (vulnResponse == null || vulnResponse.getCves() == null || vulnResponse.getCves().isEmpty()) {
                    log.info("No vulnerabilities found for service: {}", service.getName());
                    allLogs.add("NO_VULNS: " + service.getName());
                    continue;
                }

                log.info("Found {} CVEs for service: {}", vulnResponse.getCves().size(), service.getName());

                for (VulnerabilityLookupResponse.CveEntry cve : vulnResponse.getCves()) {
                    totalCves++;
                    String cveId = cve.getCveId();
                    log.info("Processing CVE: {}", cveId);

                    try {
                        String packageName = service.getName();
                        String currentVersion = service.getVersion();

                        if (isKernelPackage(packageName)) {
                            log.info("CVE {} is a kernel update, marking as SKIPPED", cveId);
                            skippedCount++;
                            reportRemediation(cveId, targetId, "KERNEL_UPDATE", "SKIPPED",
                                    packageName, currentVersion, null, "Kernel updates require manual intervention",
                                    List.of(), List.of(), List.of(), null, null);
                            allLogs.add("SKIPPED: " + cveId + " (kernel update)");
                            continue;
                        }

                        RemediationStrategyRequest strategyRequest = RemediationStrategyRequest.builder()
                                .cveId(cveId)
                                .packageName(packageName)
                                .currentVersion(currentVersion)
                                .operatingSystem(operatingSystem)
                                .build();

                        RemediationStrategyResponse strategy = httpClient.requestRemediationStrategy(strategyRequest);

                        if (strategy == null || !strategy.isFound()) {
                            log.warn("No remediation strategy found for CVE: {}", cveId);
                            failedCount++;
                            reportRemediation(cveId, targetId, "UNKNOWN", "FAILED",
                                    packageName, currentVersion, null, "No strategy available",
                                    List.of(), List.of(), List.of(), "No remediation strategy available", null);
                            allLogs.add("FAILED: " + cveId + " (no strategy)");
                            continue;
                        }

                        log.info("Executing remediation for CVE: {} on package: {}", cveId, packageName);

                        List<String> preCheckLogs = executeCommands(strategy.getPreCheckCommands());
                        List<String> executionLogs = executeCommands(strategy.getFixCommands());
                        List<String> postCheckLogs = executeCommands(strategy.getPostCheckCommands());

                        boolean requiresReboot = strategy.isRequiresReboot();
                        String status = requiresReboot ? "PENDING_REBOOT" : "SUCCESS";
                        String remediationType = requiresReboot ? "REBOOT_REQUIRED" : "SERVICE_UPDATE";

                        if (requiresReboot) {
                            pendingRebootCount++;
                            log.info("CVE {} remediation applied, reboot required", cveId);
                        } else {
                            successCount++;
                            log.info("CVE {} remediation completed successfully", cveId);
                        }

                        String actionDescription = String.format("Applied %s to %s",
                                strategy.getAction(), packageName);

                        reportRemediation(cveId, targetId, remediationType, status,
                                packageName, currentVersion, strategy.getTargetVersion(), actionDescription,
                                preCheckLogs, executionLogs, postCheckLogs, null, null);

                        allLogs.add(status + ": " + cveId + " (" + remediationType + ")");

                    } catch (Exception e) {
                        log.error("Failed to remediate CVE {}: {}", cveId, e.getMessage(), e);
                        failedCount++;
                        allLogs.add("FAILED: " + cveId + " (" + e.getMessage() + ")");
                    }
                }

            } catch (Exception e) {
                log.error("Failed to check vulnerabilities for service {}: {}",
                        service.getName(), e.getMessage(), e);
                allLogs.add("ERROR: " + service.getName() + " (" + e.getMessage() + ")");
            }
        }

        String message = String.format("Processed %d CVEs: %d SUCCESS, %d FAILED, %d SKIPPED, %d PENDING_REBOOT",
                totalCves, successCount, failedCount, skippedCount, pendingRebootCount);

        log.info(message);

        if (failedCount > 0) {
            return StepResult.failure(action, allLogs);
        }
        return StepResult.success(action, services, List.of(), allLogs);
    }

    private StepResult handleContainerSkip(StepAction action, ContainerDetectionResult detection,
                                            Map<StepAction, StepResult> context) {
        List<String> logs = new ArrayList<>();
        logs.add("CONTAINER_DETECTED: runtime="
                + (detection.getRuntimeName() != null ? detection.getRuntimeName() : "unknown"));
        logs.add("CONTAINER_DETECTED: confidence=" + detection.getConfidence());
        logs.add("CONTAINER_DETECTED: indicators=" + detection.getMatchedIndicators());
        logs.add("CONTAINER_DETECTED: method=" + detection.getDetectionMethod());

        String skipReason = buildSkipReason(detection);
        String targetId = "unknown";

        // Extract targetId from context if available
        StepResult serviceScanResult = context.get(StepAction.SERVICE_SCAN);
        if (serviceScanResult != null) {
            targetId = extractFromLogs(serviceScanResult.getLogs(), "targetId:");
        }

        reportContainerSkipRemediation(skipReason, detection, targetId);

        logs.add("ACTION: Remediation skipped — " + skipReason);

        log.info("Container skip complete: {}", skipReason);
        return StepResult.skipped(action, logs);
    }

    private String buildSkipReason(ContainerDetectionResult detection) {
        if (detection.getConfidence() == DetectionConfidence.INCONCLUSIVE) {
            return "Container detection inconclusive — remediation skipped as precaution";
        }

        String runtime = detection.getRuntimeName();
        if ("docker".equalsIgnoreCase(runtime)) {
            return "Docker container detected — remediation skipped to avoid ineffective "
                    + "or destructive changes in an ephemeral environment";
        }

        return "Container environment detected"
                + (runtime != null ? " (runtime: " + runtime + ")" : "")
                + " — remediation skipped to avoid ineffective changes in an ephemeral environment";
    }

    private void reportContainerSkipRemediation(String skipReason, ContainerDetectionResult detection,
                                                  String targetId) {
        try {
            List<String> detectionLogs = new ArrayList<>();
            detectionLogs.add("DETECTION: method=" + detection.getDetectionMethod());
            detectionLogs.add("DETECTION: runtime="
                    + (detection.getRuntimeName() != null ? detection.getRuntimeName() : "unknown"));
            detectionLogs.add("DETECTION: confidence=" + detection.getConfidence());
            detectionLogs.add("DETECTION: indicators=" + detection.getMatchedIndicators());

            RemediationReportRequest request = RemediationReportRequest.builder()
                    .cveId("CONTAINER-DETECTED")
                    .targetId(targetId)
                    .remediationType("CONTAINER_DETECTED")
                    .status("SKIPPED")
                    .actionDescription("Remediation skipped: container runtime detected")
                    .preCheckLogs(detectionLogs)
                    .executionLogs(List.of())
                    .postCheckLogs(List.of())
                    .skipReason(skipReason)
                    .build();

            httpClient.reportRemediationResult(request);
            log.info("Reported container-skip remediation for target: {}", targetId);
        } catch (Exception e) {
            log.error("Failed to report container-skip remediation: {}", e.getMessage(), e);
        }
    }

    private boolean isKernelPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        return packageName.matches(KERNEL_PACKAGE_PATTERN);
    }

    private List<String> executeCommands(List<String> commands) {
        List<String> logs = new ArrayList<>();
        if (commands == null || commands.isEmpty()) {
            return logs;
        }

        for (String command : commands) {
            log.debug("Executing command: {}", command);
            try {
                TaskResult result = commandExecutor.execute(command, COMMAND_TIMEOUT_SECONDS);
                logs.add("$ " + command);
                if (result.isSuccess()) {
                    if (result.getMessage() != null && !result.getMessage().isBlank()) {
                        logs.add(result.getMessage());
                    }
                } else {
                    logs.add("ERROR: " + result.getFailureReason());
                    log.error("Command failed: {} - {}", command, result.getFailureReason());
                }
            } catch (Exception e) {
                log.error("Command execution failed: {}", command, e);
                logs.add("$ " + command);
                logs.add("ERROR: " + e.getMessage());
            }
        }

        return logs;
    }

    private void reportRemediation(String cveId, String targetId, String remediationType, String status,
                                    String packageName, String packageVersionBefore, String packageVersionAfter,
                                    String actionDescription, List<String> preCheckLogs, List<String> executionLogs,
                                    List<String> postCheckLogs, String errorMessage, String rollbackHint) {
        try {
            RemediationReportRequest request = RemediationReportRequest.builder()
                    .cveId(cveId)
                    .targetId(targetId)
                    .remediationType(remediationType)
                    .status(status)
                    .packageName(packageName)
                    .packageVersionBefore(packageVersionBefore)
                    .packageVersionAfter(packageVersionAfter)
                    .actionDescription(actionDescription)
                    .preCheckLogs(preCheckLogs)
                    .executionLogs(executionLogs)
                    .postCheckLogs(postCheckLogs)
                    .errorMessage(errorMessage)
                    .rollbackHint(rollbackHint)
                    .build();

            httpClient.reportRemediationResult(request);
            log.info("Reported remediation result for CVE: {}", cveId);
        } catch (Exception e) {
            log.error("Failed to report remediation result for CVE {}: {}", cveId, e.getMessage(), e);
        }
    }

    private String extractFromLogs(List<String> logs, String prefix) {
        if (logs == null) {
            return "unknown";
        }
        return logs.stream()
                .filter(l -> l.startsWith(prefix))
                .findFirst()
                .map(l -> l.substring(prefix.length()).trim())
                .orElse("unknown");
    }

    private String detectOperatingSystem() {
        String osName = System.getProperty("os.name", "").toLowerCase();

        if (osName.contains("linux")) {
            String distro = detectLinuxDistro();
            if (distro != null) {
                return distro;
            }
            // Fall back to the kernel string when /etc/os-release is unavailable.
            return "linux-" + System.getProperty("os.version", "");
        }
        return osName + "-" + System.getProperty("os.version", "");
    }

    /**
     * Reads {@code /etc/os-release} and builds the distribution identifier the
     * remediation strategy catalog is keyed by, e.g. {@code ubuntu-22.04} or
     * {@code debian-12}. Returns {@code null} when the file is missing or lacks
     * the required fields so the caller can fall back to the kernel string.
     */
    private String detectLinuxDistro() {
        java.nio.file.Path osRelease = java.nio.file.Path.of("/etc/os-release");
        if (!java.nio.file.Files.isReadable(osRelease)) {
            return null;
        }
        try {
            return parseDistro(java.nio.file.Files.readAllLines(osRelease));
        } catch (java.io.IOException e) {
            log.warn("Failed to read /etc/os-release: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses {@code ID}/{@code VERSION_ID} from {@code /etc/os-release} lines into
     * the distro identifier the strategy catalog uses (e.g. {@code ubuntu-22.04}).
     * Returns {@code null} when either field is absent.
     */
    static String parseDistro(List<String> osReleaseLines) {
        String id = null;
        String versionId = null;
        for (String line : osReleaseLines) {
            if (line.startsWith("ID=")) {
                id = unquote(line.substring("ID=".length()));
            } else if (line.startsWith("VERSION_ID=")) {
                versionId = unquote(line.substring("VERSION_ID=".length()));
            }
        }
        if (id == null || id.isBlank() || versionId == null || versionId.isBlank()) {
            return null;
        }
        return id.toLowerCase() + "-" + versionId;
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2
                && (trimmed.charAt(0) == '"' || trimmed.charAt(0) == '\'')
                && trimmed.charAt(trimmed.length() - 1) == trimmed.charAt(0)) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
