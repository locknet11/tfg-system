package com.spulido.agent.worker.step;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.remote.RemoteCommandExecutor;
import com.spulido.agent.worker.BinaryIntegrityVerifier;
import com.spulido.agent.worker.ScriptTemplateService;
import com.spulido.agent.worker.http.AgentHttpClient;

class TransferAgentStepHandlerTest {

    private TransferAgentStepHandler handler;
    private AgentHttpClient mockHttpClient;
    private RemoteCommandExecutor mockRemoteExecutor;
    private BinaryIntegrityVerifier mockVerifier;
    private ScriptTemplateService mockTemplate;
    private AgentConfig config;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(AgentHttpClient.class);
        mockRemoteExecutor = mock(RemoteCommandExecutor.class);
        mockVerifier = mock(BinaryIntegrityVerifier.class);
        mockTemplate = mock(ScriptTemplateService.class);
        config = new AgentConfig();
        config.setExploitTransferMethod("auto");
        config.setExploitTransferMethodRetries(2);
        handler = new TransferAgentStepHandler(mockHttpClient, mockRemoteExecutor, mockVerifier,
                mockTemplate, config);
    }

    private Map<StepAction, StepResult> buildContext(String targetIp, String targetUser,
                                                      String downloadUrl, String preauthCode, String centralUrl) {
        Map<StepAction, StepResult> context = new HashMap<>();
        context.put(StepAction.EXECUTE_EXPLOIT, new StepResult(StepAction.EXECUTE_EXPLOIT,
                List.of(), List.of(),
                List.of("targetIp:" + targetIp, "targetUser:" + targetUser, "reverseShellActive:true"),
                true, false));
        context.put(StepAction.REQUEST_REPLICATION, new StepResult(StepAction.REQUEST_REPLICATION,
                List.of(), List.of(),
                List.of("downloadUrl:" + downloadUrl, "preauthCode:" + preauthCode,
                        "centralUrl:" + centralUrl),
                true, false));
        return context;
    }

    @Test
    void pathABasicSuccess() {
        Map<StepAction, StepResult> context = buildContext(
                "172.31.128.4", "root", "https://central/api/agent/binary/token123",
                "preauth-abc", "https://central");

        when(mockRemoteExecutor.execute(any(), anyString(), anyLong()))
                .thenReturn(
                        TaskResult.success("probe1", "curl 7.68.0"),
                        TaskResult.success("probe2", "wget 1.21"),
                        TaskResult.success("probe3", "central reachable"),
                        TaskResult.success("install", "INSTALL_OK\n"),
                        TaskResult.success("health", "{\"status\":\"UP\"}"));

        when(mockRemoteExecutor.transferFile(any(), any(), anyString(), anyString()))
                .thenReturn(TaskResult.success("transfer", "File transferred"));

        when(mockTemplate.renderTemplate(anyString(), any()))
                .thenReturn("#!/bin/bash\necho INSTALL_OK");

        StepResult result = handler.handle(StepAction.TRANSFER_AGENT, context, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("HTTP_DOWNLOAD")));
    }

    @Test
    void pathBWhenTargetHasNoTools() {
        Map<StepAction, StepResult> context = buildContext(
                "172.31.128.4", "root", "https://central/api/agent/binary/token123",
                "preauth-abc", "https://central");

        when(mockRemoteExecutor.execute(any(), anyString(), anyLong()))
                .thenReturn(
                        TaskResult.failure("p1", "curl not found", "exit 1"),
                        TaskResult.failure("p2", "wget not found", "exit 1"),
                        TaskResult.failure("p3", "central unreachable", "exit 7"),
                        TaskResult.success("launch", "INSTALL_OK\n"),
                        TaskResult.success("health", "{\"status\":\"UP\"}"));

        when(mockRemoteExecutor.transferFile(any(), any(), anyString(), anyString()))
                .thenReturn(TaskResult.success("tr1", "File transferred via SCP"),
                        TaskResult.success("tr2", "File transferred"));

        when(mockHttpClient.downloadBinary(anyString()))
                .thenReturn("binary-data".getBytes());

        when(mockVerifier.verify(any(byte[].class))).thenReturn(true);

        when(mockTemplate.renderTemplate(anyString(), any()))
                .thenReturn("#!/bin/bash\necho INSTALL_OK");

        StepResult result = handler.handle(StepAction.TRANSFER_AGENT, context, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("AGENT_PUSH")));
    }

    @Test
    void fallbackFromPathAToPathB() {
        Map<StepAction, StepResult> context = buildContext(
                "172.31.128.4", "root", "https://central/api/agent/binary/token123",
                "preauth-abc", "https://central");

        when(mockRemoteExecutor.execute(any(), anyString(), anyLong()))
                .thenReturn(
                        TaskResult.success("p1", "curl 7.68.0"),
                        TaskResult.success("p2", "wget 1.21"),
                        TaskResult.success("p3", "central ok"),
                        TaskResult.failure("a1", "download failed", "exit 1"),
                        TaskResult.failure("a2", "download failed again", "exit 1"),
                        TaskResult.success("launch", "INSTALL_OK\n"),
                        TaskResult.success("health", "{\"status\":\"UP\"}"));

        when(mockRemoteExecutor.transferFile(any(), any(), anyString(), anyString()))
                .thenReturn(TaskResult.success("tr1", "File transferred"),
                        TaskResult.success("tr2", "File transferred"),
                        TaskResult.success("tr3", "File transferred"));

        when(mockHttpClient.downloadBinary(anyString()))
                .thenReturn("binary-data".getBytes());

        when(mockVerifier.verify(any(byte[].class))).thenReturn(true);

        when(mockTemplate.renderTemplate(anyString(), any()))
                .thenReturn("#!/bin/bash\necho INSTALL_OK");

        StepResult result = handler.handle(StepAction.TRANSFER_AGENT, context, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("fallback_to_B")));
    }

    @Test
    void healthCheckPartialSuccess() {
        Map<StepAction, StepResult> context = buildContext(
                "172.31.128.4", "root", "https://central/api/agent/binary/token123",
                "preauth-abc", "https://central");

        when(mockRemoteExecutor.execute(any(), anyString(), anyLong()))
                .thenReturn(
                        TaskResult.success("p1", "curl 7.68.0"),
                        TaskResult.success("p2", "wget 1.21"),
                        TaskResult.success("p3", "central ok"),
                        TaskResult.success("install", "INSTALL_OK\n"),
                        TaskResult.failure("hc1", "not ready", "exit 7"),
                        TaskResult.failure("hc2", "not ready", "exit 7"),
                        TaskResult.failure("hc3", "not ready", "exit 7"));

        when(mockRemoteExecutor.transferFile(any(), any(), anyString(), anyString()))
                .thenReturn(TaskResult.success("tr1", "File transferred"));

        when(mockTemplate.renderTemplate(anyString(), any()))
                .thenReturn("#!/bin/bash\necho INSTALL_OK");

        StepResult result = handler.handle(StepAction.TRANSFER_AGENT, context, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("PARTIAL_SUCCESS")));
    }

    @Test
    void integrityFailure() {
        Map<StepAction, StepResult> context = buildContext(
                "172.31.128.4", "root", "https://central/api/agent/binary/token123",
                "preauth-abc", "https://central");

        when(mockRemoteExecutor.execute(any(), anyString(), anyLong()))
                .thenReturn(
                        TaskResult.failure("probe1", "no curl", "exit 1"),
                        TaskResult.failure("probe2", "no wget", "exit 1"));

        when(mockHttpClient.downloadBinary(anyString()))
                .thenReturn("corrupted-data".getBytes());

        when(mockVerifier.verify(any(byte[].class))).thenReturn(false);

        StepResult result = handler.handle(StepAction.TRANSFER_AGENT, context, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("integrity verification failed")));
    }
}
