package com.spulido.agent.worker.step;

import java.util.List;
import java.util.Map;

import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;

public class EchoStepHandler implements StepHandler {

    @Override
    public StepResult handle(StepAction action, Map<StepAction, StepResult> context) {
        return StepResult.success(action, List.of(), List.of(),
                List.of("Executed echo step for action: " + action));
    }
}
