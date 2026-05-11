package com.spulido.agent.worker.step;

import java.util.Map;

import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;

public interface StepHandler {

    StepResult handle(StepAction action, Map<StepAction, StepResult> context);
}
