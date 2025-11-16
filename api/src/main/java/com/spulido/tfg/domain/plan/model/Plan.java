package com.spulido.tfg.domain.plan.model;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

// This value object describes the plan to be executed by an agent or derived from a template
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class Plan {

    @Field
    private String notes;

    @Field
    private boolean allowTemplating;

    @Field
    private List<Step> steps;
}
