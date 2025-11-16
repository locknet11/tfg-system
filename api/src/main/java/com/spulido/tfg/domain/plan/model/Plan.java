package com.spulido.tfg.domain.plan.model;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.spulido.tfg.domain.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

// This entity describes the plan to be executed by an agent

@Document(collection = "plans")
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class Plan extends BaseEntity {

    @Field
    private String notes;

    @Field
    private boolean allowTemplating;

    @Field
    private List<Step> steps;

}
