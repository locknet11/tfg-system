package com.spulido.agent.worker.step;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.spulido.agent.container.ContainerDetectionResult;
import com.spulido.agent.container.ContainerDetector;
import com.spulido.agent.container.DetectionConfidence;
import com.spulido.agent.container.DetectionMethod;
import com.spulido.agent.domain.task.ServiceInfo;
import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.worker.CommandExecutor;
import com.spulido.agent.worker.http.AgentHttpClient;

/**
 * Regression test confirming that RemediationStepHandler works correctly in
 * the common path where no vulnerabilities are found for a service.
 */
class RemediationStepHandlerCommandResolutionTest {

    private RemediationStepHandler handler;
    private AgentHttpClient mockHttpClient;
    private CommandExecutor mockExecutor;
    private ContainerDetector mockDetector;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(AgentHttpClient.class);
        mockExecutor = mock(CommandExecutor.class);
        mockDetector = mock(ContainerDetector.class);

        when(mockDetector.detect()).thenReturn(new ContainerDetectionResult(
                false, DetectionConfidence.CONFIRMED, DetectionMethod.NONE,
                Set.of(), null));

        handler = new RemediationStepHandler(mockHttpClient, mockExecutor, mockDetector);
    }

    @Test
    void remediationHandlerCompletesWithoutErrorWhenNoVulnsFound() {
        ServiceInfo service = new ServiceInfo("curl", "7.68.0", 80);
        Map<StepAction, StepResult> context = new HashMap<>();
        context.put(StepAction.SERVICE_SCAN,
                new StepResult(StepAction.SERVICE_SCAN, List.of(service), List.of(),
                        List.of("targetId:172.31.128.4"), true, false));

        when(mockHttpClient.lookupVulnerabilities(any()))
                .thenReturn(null);

        StepResult result = handler.handle(StepAction.REMEDIATE, context, "172.31.128.4");

        assertTrue(result.isSuccess());
    }
}
