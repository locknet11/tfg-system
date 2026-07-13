package com.spulido.agent.worker.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import com.spulido.agent.domain.task.ServiceInfo;
import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.worker.CommandExecutor;

class ServiceScanStepHandlerTest {

    private ServiceScanStepHandler handler;
    private CommandExecutor mockExecutor;

    @BeforeEach
    void setUp() {
        mockExecutor = mock(CommandExecutor.class);
        handler = new ServiceScanStepHandler(mockExecutor);
    }

    @Test
    void handleSuccessPopulatesServices() {
        String nmapOutput = "Command completed. Output: \n"
                + "Starting Nmap\n"
                + "PORT     STATE SERVICE VERSION\n"
                + "22/tcp   open  ssh     OpenSSH 8.9 (protocol 2.0)\n"
                + "80/tcp   open  http    Apache httpd 2.4.57\n"
                + "443/tcp  open  https   nginx 1.24.0\n"
                + "Nmap done: 1 IP address scanned";

        when(mockExecutor.execute(anyString(), anyLong()))
                .thenReturn(TaskResult.success("nmap-1", nmapOutput));

        Map<StepAction, StepResult> context = new HashMap<>();
        StepResult result = handler.handle(StepAction.SERVICE_SCAN, context, "172.31.128.4");

        assertTrue(result.isSuccess());
        List<ServiceInfo> services = result.getServices();
        assertEquals(3, services.size());

        ServiceInfo ssh = services.get(0);
        assertEquals("ssh", ssh.getName());
        assertTrue(ssh.getVersion().contains("OpenSSH"));
        assertEquals(22, ssh.getPort());

        ServiceInfo http = services.get(1);
        assertEquals("http", http.getName());
        assertEquals(80, http.getPort());

        ServiceInfo https = services.get(2);
        assertEquals("https", https.getName());
        assertEquals(443, https.getPort());

        assertTrue(result.getLogs().stream().anyMatch(l -> l.startsWith("targetId:")),
                "Should include targetId in logs");
        assertTrue(result.getLogs().stream().anyMatch(l -> l.startsWith("SERVICE:")),
                "Should include SERVICE entries in logs");
    }

    @Test
    void handleReportsNoServicesWhenNoneFound() {
        String nmapOutput = "Command completed. Output: \n"
                + "All 1000 scanned ports are closed";

        when(mockExecutor.execute(anyString(), anyLong()))
                .thenReturn(TaskResult.success("nmap-1", nmapOutput));

        Map<StepAction, StepResult> context = new HashMap<>();
        StepResult result = handler.handle(StepAction.SERVICE_SCAN, context, "172.31.128.4");

        assertTrue(result.isSuccess());
        assertTrue(result.getServices().isEmpty());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("No open services")),
                "Should report no open services");
    }

    @Test
    void handleFailsWhenExecutorFails() {
        when(mockExecutor.execute(anyString(), anyLong()))
                .thenReturn(TaskResult.failure("nmap-1", "nmap failed", "exit code 1"));

        Map<StepAction, StepResult> context = new HashMap<>();
        StepResult result = handler.handle(StepAction.SERVICE_SCAN, context, "172.31.128.4");

        assertFalse(result.isSuccess());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.startsWith("TOOL_ERROR:nmap:")),
                "Should contain TOOL_ERROR log");
    }

    @Test
    void handleFailsWhenNoTargetIp() {
        Map<StepAction, StepResult> context = new HashMap<>();
        StepResult result = handler.handle(StepAction.SERVICE_SCAN, context, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("TOOL_ERROR:nmap:no_target")));
    }

    @Test
    void parseServiceLineReturnsServiceInfoForValidLine() {
        ServiceInfo info = ServiceScanStepHandler.parseServiceLine(
                "22/tcp   open  ssh     OpenSSH 8.9 (protocol 2.0)");

        assertNotNull(info);
        assertEquals("ssh", info.getName());
        assertEquals(22, info.getPort());
        assertTrue(info.getVersion().contains("OpenSSH"));
    }

    @Test
    void parseServiceLineReturnsNullForInvalidLine() {
        assertTrue(ServiceScanStepHandler.parseServiceLine("Not a valid line") == null);
        assertTrue(ServiceScanStepHandler.parseServiceLine("") == null);
    }

    @Test
    void parseServiceScanOutputReturnsServicesForNmapOutput() {
        String output = "PORT     STATE SERVICE VERSION\n"
                + "22/tcp   open  ssh     OpenSSH 8.9\n"
                + "80/tcp   open  http    Apache httpd 2.4.57\n";

        List<ServiceInfo> services = ServiceScanStepHandler.parseServiceScanOutput(output);

        assertEquals(2, services.size());
        assertEquals("ssh", services.get(0).getName());
        assertEquals("http", services.get(1).getName());
    }
}
