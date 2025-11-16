package com.spulido.tfg.domain.plan.model;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Field;

public class Step {

    @Field
    private StepExecutionStatus status;

    @Field
    private StepAction action;

    @Field
    private List<String> logs;

}
