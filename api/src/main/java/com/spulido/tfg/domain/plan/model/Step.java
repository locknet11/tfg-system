package com.spulido.tfg.domain.plan.model;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Step {

    @Field
    private StepExecutionStatus status = StepExecutionStatus.PENDING;

    @Field
    private StepAction action;

    @Field
    private List<String> logs = List.of();
}
