package com.spulido.agent.teardown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.utils.AgentLifecycle;
import com.spulido.agent.worker.ScriptTemplateService;
import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.TeardownReportRequest;

@ExtendWith(MockitoExtension.class)
class TeardownServiceTest {

    @Mock
    private AgentConfig config;
    @Mock
    private ArtifactSet artifactSet;
    @Mock
    private ScriptTemplateService scriptTemplateService;
    @Mock
    private DetachedCleanupLauncher cleanupLauncher;
    @Mock
    private AgentLifecycle agentLifecycle;
    @Mock
    private AgentHttpClient httpClient;

    private TeardownService service;

    @BeforeEach
    void setUp() {
        service = new TeardownService(config, artifactSet, scriptTemplateService,
                cleanupLauncher, agentLifecycle, httpClient);
        when(config.getAgentId()).thenReturn("agent-1");
        when(scriptTemplateService.renderTemplate(anyString(), anyMap())).thenReturn("#!/bin/sh\n");
    }

    private void stubResolve(Map<ArtifactType, Path> paths) {
        when(artifactSet.resolve()).thenReturn(paths);
    }

    @Test
    void selfDestruct_reportsOutcomeBeforeSpawningCleanupAndStopping() {
        stubResolve(new EnumMap<>(ArtifactType.class));
        when(artifactSet.remove(any())).thenReturn(RemovalStatus.NOT_PRESENT);

        service.selfDestruct(TeardownTrigger.PLAN_COMPLETION);

        InOrder inOrder = Mockito.inOrder(httpClient, cleanupLauncher, agentLifecycle);
        inOrder.verify(httpClient).reportTeardownOutcome(any(TeardownReportRequest.class));
        inOrder.verify(cleanupLauncher).launch(anyString());
        inOrder.verify(agentLifecycle).stop();
    }

    @Test
    void selfDestruct_isSingleShot() {
        stubResolve(new EnumMap<>(ArtifactType.class));
        when(artifactSet.remove(any())).thenReturn(RemovalStatus.NOT_PRESENT);

        service.selfDestruct(TeardownTrigger.PLAN_COMPLETION);
        service.selfDestruct(TeardownTrigger.PLATFORM_DEPROVISION);

        verify(agentLifecycle, times(1)).stop();
        verify(cleanupLauncher, times(1)).launch(anyString());
        assertThat(service.isTearingDown()).isTrue();
    }

    @Test
    void selfDestruct_bestEffort_oneFailedRemovalDoesNotAbortTheRest() {
        Map<ArtifactType, Path> paths = new EnumMap<>(ArtifactType.class);
        paths.put(ArtifactType.AGENT_CONFIG, Path.of("/tmp/agent.properties"));
        paths.put(ArtifactType.AGENT_LOG, Path.of("/tmp/agent.log"));
        stubResolve(paths);
        when(artifactSet.remove(Path.of("/tmp/agent.properties"))).thenReturn(RemovalStatus.FAILED);
        when(artifactSet.remove(Path.of("/tmp/agent.log"))).thenReturn(RemovalStatus.REMOVED);
        when(artifactSet.remove(null)).thenReturn(RemovalStatus.NOT_PRESENT);

        service.selfDestruct(TeardownTrigger.PLAN_COMPLETION);

        // All reportable artifacts attempted; both config and log were removed-attempted.
        verify(artifactSet).remove(Path.of("/tmp/agent.properties"));
        verify(artifactSet).remove(Path.of("/tmp/agent.log"));
        verify(agentLifecycle).stop();

        ArgumentCaptor<TeardownReportRequest> captor = ArgumentCaptor.forClass(TeardownReportRequest.class);
        verify(httpClient).reportTeardownOutcome(captor.capture());
        assertThat(captor.getValue().getResults())
                .anyMatch(r -> r.getStatus().equals(RemovalStatus.FAILED.name()));
        assertThat(captor.getValue().getBinaryRemoval())
                .isEqualTo(TeardownOutcome.BINARY_PENDING_DETACHED);
    }

    @Test
    void selfDestruct_stillExitsWhenReportFails() {
        stubResolve(new EnumMap<>(ArtifactType.class));
        when(artifactSet.remove(any())).thenReturn(RemovalStatus.NOT_PRESENT);
        Mockito.doThrow(new RuntimeException("offline"))
                .when(httpClient).reportTeardownOutcome(any());

        service.selfDestruct(TeardownTrigger.PLATFORM_DEPROVISION);

        verify(cleanupLauncher).launch(anyString());
        verify(agentLifecycle).stop();
    }
}
