package com.spulido.agent.worker.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.worker.CommandExecutor;

/**
 * Step handler for NETWORK_SCAN action.
 * Uses the bundled {@code nmap} binary for host discovery against the plan's
 * target IP, producing {@code HOST_FOUND:<address>} log entries for every
 * reachable host discovered.
 */
public class NetworkScanStepHandler implements StepHandler {

    private static final Logger log = LoggerFactory.getLogger(NetworkScanStepHandler.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 120;

    private final CommandExecutor commandExecutor;

    public NetworkScanStepHandler(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public StepResult handle(StepAction action, Map<StepAction, StepResult> context, String targetIp) {
        log.info("Starting NETWORK_SCAN against target: {}", targetIp);

        if (targetIp == null || targetIp.isBlank()) {
            log.warn("No target IP provided for network scan");
            return StepResult.failure(action, List.of("TOOL_ERROR:nmap:no_target"));
        }

        String command = "nmap -sn " + targetIp;

        try {
            TaskResult result = commandExecutor.execute(command, DEFAULT_TIMEOUT_SECONDS);
            List<String> logs = new ArrayList<>();

            if (result.isSuccess()) {
                String output = result.getMessage() != null ? result.getMessage() : "";
                List<String> discoveredHosts = parseHostDiscoveryOutput(output, targetIp);

                if (discoveredHosts.isEmpty()) {
                    log.info("No hosts discovered on target {}", targetIp);
                    logs.add("No reachable hosts discovered");
                } else {
                    for (String host : discoveredHosts) {
                        logs.add("HOST_FOUND:" + host);
                    }
                    log.info("Discovered {} host(s) on target {}", discoveredHosts.size(), targetIp);
                }
                return StepResult.success(action, List.of(), List.of(), logs);
            } else {
                String reason = result.getFailureReason() != null ? result.getFailureReason() : "unknown";
                log.error("nmap host discovery failed: {}", reason);
                logs.add("TOOL_ERROR:nmap:" + reason);
                return StepResult.failure(action, logs);
            }
        } catch (Exception e) {
            log.error("Network scan execution failed", e);
            List<String> logs = new ArrayList<>();
            logs.add("TOOL_ERROR:nmap:" + e.getMessage());
            return StepResult.failure(action, logs);
        }
    }

    /**
     * Parses nmap -sn (ping scan) output and extracts discovered host addresses.
     * Looks for lines matching "Nmap scan report for <hostname or IP>".
     */
    static List<String> parseHostDiscoveryOutput(String output, String targetIp) {
        List<String> hosts = new ArrayList<>();
        if (output == null || output.isBlank()) {
            return hosts;
        }
        for (String line : output.split("\\R")) {
            if (line.startsWith("Nmap scan report for ")) {
                String address = line.substring("Nmap scan report for ".length()).trim();
                if (!address.isEmpty()) {
                    hosts.add(address);
                }
            }
        }
        return hosts;
    }
}
