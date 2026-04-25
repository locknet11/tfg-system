package com.spulido.agent.worker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.spulido.agent.domain.task.TaskDefinition;

public class JobValidator {

    public static void validate(List<TaskDefinition> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Job must contain at least one executable step");
        }

        Set<Integer> orders = new HashSet<>();
        for (TaskDefinition step : steps) {
            if (!orders.add(step.getOrder())) {
                throw new IllegalArgumentException("Duplicate task order: " + step.getOrder());
            }
        }
    }
}
