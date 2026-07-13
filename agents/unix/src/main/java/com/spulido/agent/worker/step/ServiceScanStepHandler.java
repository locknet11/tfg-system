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

/**
 * Step handler for SERVICE_SCAN and SYSTEM_SCAN actions.
 * Uses the bundled {@code nmap} binary for service and version detection
 * against the plan's target IP, populating {@link ServiceInfo} entries
 * with discovered open ports, services, and versions.
 */
public class ServiceScanStepHandler implements StepHandler {

    private static final Logger log = LoggerFactory.getLogger(ServiceScanStepHandler.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 300;

    private final CommandExecutor commandExecutor;

    public ServiceScanStepHandler(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public StepResult handle(StepAction action, Map<StepAction, StepResult> context, String targetIp) {
        log.info("Starting {} against target: {}", action, targetIp);

        if (targetIp == null || targetIp.isBlank()) {
            log.warn("No target IP provided for {}", action);
            return StepResult.failure(action, List.of("TOOL_ERROR:nmap:no_target"));
        }

        // Fast service/version scan; -T4 speeds it up, -sV enables version detection.
        // We scan common ports (top 1000 by default) to keep scan time reasonable.
        String command = "nmap -sV -T4 " + targetIp;

        try {
            TaskResult result = commandExecutor.execute(command, DEFAULT_TIMEOUT_SECONDS);
            List<String> logs = new ArrayList<>();

            if (result.isSuccess()) {
                String output = result.getMessage() != null ? result.getMessage() : "";
                List<ServiceInfo> services = parseServiceScanOutput(output);

                if (services.isEmpty()) {
                    log.info("No open services discovered on target {}", targetIp);
                    logs.add("No open services discovered");
                } else {
                    logs.add("targetId:" + targetIp);
                    for (ServiceInfo svc : services) {
                        logs.add("SERVICE:" + svc.getName() + ":" + svc.getVersion() + ":" + svc.getPort());
                        log.info("Discovered service: {} {} on port {}", svc.getName(), svc.getVersion(), svc.getPort());
                    }
                }
                return StepResult.success(action, services, List.of(), logs);
            } else {
                String reason = result.getFailureReason() != null ? result.getFailureReason() : "unknown";
                log.error("nmap service scan failed: {}", reason);
                logs.add("TOOL_ERROR:nmap:" + reason);
                return StepResult.failure(action, logs);
            }
        } catch (Exception e) {
            log.error("Service scan execution failed", e);
            List<String> logs = new ArrayList<>();
            logs.add("TOOL_ERROR:nmap:" + e.getMessage());
            return StepResult.failure(action, logs);
        }
    }

    /**
     * Parses nmap -sV (service/version scan) output into ServiceInfo entries.
     * Extracts open ports with their service name and version from lines like:
     * {@code 22/tcp   open  ssh     OpenSSH 8.9 (protocol 2.0)}.
     */
    static List<ServiceInfo> parseServiceScanOutput(String output) {
        List<ServiceInfo> services = new ArrayList<>();
        if (output == null || output.isBlank()) {
            return services;
        }
        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.contains("/tcp") && !trimmed.contains("/udp")) {
                continue;
            }
            if (!trimmed.contains(" open ")) {
                continue;
            }
            try {
                ServiceInfo info = parseServiceLine(trimmed);
                if (info != null) {
                    services.add(info);
                }
            } catch (Exception e) {
                log.debug("Skipping unparseable nmap output line: {}", trimmed);
            }
        }
        return services;
    }

    /**
     * Parses a single "PORT   STATE   SERVICE [VERSION]" line.
     * Returns null if the line cannot be parsed into a valid ServiceInfo.
     */
    static ServiceInfo parseServiceLine(String line) {
        // Split on whitespace, expect at least: port/proto state service [version...]
        String[] parts = line.split("\\s+", 4);
        if (parts.length < 3) {
            return null;
        }

        String portProto = parts[0];   // e.g. "22/tcp"
        String serviceName = parts[2]; // e.g. "ssh"
        String version = "";

        if (parts.length >= 4) {
            version = parts[3].trim(); // e.g. "OpenSSH 8.9 (protocol 2.0)"
        }
        if (version.isEmpty()) {
            version = "unknown";
        }

        int slashIdx = portProto.indexOf('/');
        if (slashIdx < 0) {
            return null;
        }
        String portStr = portProto.substring(0, slashIdx);

        try {
            int port = Integer.parseInt(portStr);
            return new ServiceInfo(serviceName, version, port);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
