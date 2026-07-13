package com.spulido.agent.worker.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.worker.CommandExecutor;

class NetworkScanStepHandlerTest {

    private NetworkScanStepHandler handler;
    private CommandExecutor mockExecutor;

    @BeforeEach
    void setUp() {
        mockExecutor = mock(CommandExecutor.class);
        handler = new NetworkScanStepHandler(mockExecutor);
    }

    @Test
    void handleSuccessDiscoversHosts() {
        String nmapOutput = "Command completed. Output: \n"
                + "Starting Nmap\n"
                + "Nmap scan report for 172.31.128.4\n"
                + "Host is up (0.00020s latency).\n"
                + "Nmap scan report for 172.31.128.5\n"
                + "Host is up (0.00040s latency).\n"
                + "Nmap done: 256 IP addresses scanned";

        when(mockExecutor.execute(anyString(), anyLong()))
                .thenReturn(TaskResult.success("nmap-1", nmapOutput));

        Map<StepAction, StepResult> context = new HashMap<>();
        StepResult result = handler.handle(StepAction.NETWORK_SCAN, context, "172.31.128.0/24");

        assertTrue(result.isSuccess());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.startsWith("HOST_FOUND:")),
                "Should contain HOST_FOUND entries");
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("172.31.128.4")),
                "Should contain first discovered host IP");
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("172.31.128.5")),
                "Should contain second discovered host IP");
    }

    @Test
    void handleReportsNoHostsWhenNoneFound() {
        String nmapOutput = "Command completed. Output: \n"
                + "Starting Nmap\n"
                + "Nmap done: 256 IP addresses scanned -- 0 hosts up";

        when(mockExecutor.execute(anyString(), anyLong()))
                .thenReturn(TaskResult.success("nmap-1", nmapOutput));

        Map<StepAction, StepResult> context = new HashMap<>();
        StepResult result = handler.handle(StepAction.NETWORK_SCAN, context, "10.0.0.0/24");

        assertTrue(result.isSuccess());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("No reachable hosts")),
                "Should report no hosts discovered");
    }

    @Test
    void handleFailsWhenExecutorFails() {
        when(mockExecutor.execute(anyString(), anyLong()))
                .thenReturn(TaskResult.failure("nmap-1", "nmap failed", "exit code 1"));

        Map<StepAction, StepResult> context = new HashMap<>();
        StepResult result = handler.handle(StepAction.NETWORK_SCAN, context, "172.31.128.0/24");

        assertFalse(result.isSuccess());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.startsWith("TOOL_ERROR:nmap:")),
                "Should contain TOOL_ERROR:nmap log line");
    }

    @Test
    void handleFailsWhenNoTargetIp() {
        Map<StepAction, StepResult> context = new HashMap<>();
        StepResult result = handler.handle(StepAction.NETWORK_SCAN, context, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("TOOL_ERROR:nmap:no_target")),
                "Should report missing target error");
    }

    @Test
    void parseHostDiscoveryOutputExtractsAddresses() {
        String output = "Nmap scan report for 192.168.1.1\n"
                + "Host is up (0.0010s latency).\n"
                + "Nmap scan report for host.example.com\n"
                + "Host is up (0.0020s latency).\n";

        List<String> hosts = NetworkScanStepHandler.parseHostDiscoveryOutput(output, "192.168.1.0/24");

        assertEquals(2, hosts.size());
        assertTrue(hosts.contains("192.168.1.1"));
        assertTrue(hosts.contains("host.example.com"));
    }

    @Test
    void parseHostDiscoveryOutputReturnsEmptyForBlankInput() {
        assertTrue(NetworkScanStepHandler.parseHostDiscoveryOutput("", "10.0.0.0/24").isEmpty());
        assertTrue(NetworkScanStepHandler.parseHostDiscoveryOutput(null, "10.0.0.0/24").isEmpty());
    }
}
