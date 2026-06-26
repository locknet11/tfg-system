package com.spulido.agent.worker.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 */
public class RemediationStepHandler implements StepHandler {

    private static final Logger log = LoggerFactory.getLogger(RemediationStepHandler.class);
    private static final String KERNEL_PACKAGE_PATTERN = "^linux-(image|headers|modules)-.*";
    private static final long COMMAND_TIMEOUT_SECONDS = 300;

    private final AgentHttpClient httpClient;
    private final CommandExecutor commandExecutor;

    public RemediationStepHandler(AgentHttpClient httpClient, CommandExecutor commandExecutor) {
        this.httpClient = httpClient;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public StepResult handle(StepAction action, Map<StepAction, StepResult> context) {
        log.info("Starting remediation step handler");

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
        String osVersion = System.getProperty("os.version", "");

        if (osName.contains("linux")) {
            return "linux-" + osVersion;
        }
        return osName + "-" + osVersion;
    }
}
