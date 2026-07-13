package com.spulido.agent.teardown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.worker.step.SelfDestructStepHandler;

@ExtendWith(MockitoExtension.class)
class SelfDestructStepHandlerTest {

    @Mock
    private TeardownService teardownService;

    @Test
    void handle_delegatesToTeardownServiceWithStepTrigger() {
        SelfDestructStepHandler handler = new SelfDestructStepHandler(teardownService);

        StepResult result = handler.handle(StepAction.SELF_DESTRUCT, Map.of(), "10.0.0.5");

        verify(teardownService).selfDestruct(TeardownTrigger.SELF_DESTRUCT_STEP);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAction()).isEqualTo(StepAction.SELF_DESTRUCT);
    }
}
