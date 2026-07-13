package com.spulido.agent.worker.step;

import java.util.List;
import java.util.Map;

import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.teardown.TeardownService;
import com.spulido.agent.teardown.TeardownTrigger;

/**
 * Handles an explicit {@code SELF_DESTRUCT} plan step by delegating to the
 * {@link TeardownService}. Teardown itself is single-shot and best-effort.
 */
public class SelfDestructStepHandler implements StepHandler {

    private final TeardownService teardownService;

    public SelfDestructStepHandler(TeardownService teardownService) {
        this.teardownService = teardownService;
    }

    @Override
    public StepResult handle(StepAction action, Map<StepAction, StepResult> context, String targetIp) {
        teardownService.selfDestruct(TeardownTrigger.SELF_DESTRUCT_STEP);
        return StepResult.success(action, List.of(), List.of(),
                List.of("Self-destruction initiated"));
    }
}
