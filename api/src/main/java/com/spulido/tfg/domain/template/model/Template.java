package com.spulido.tfg.domain.template.model;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.spulido.tfg.domain.BaseEntity;
import com.spulido.tfg.domain.ScopedEntity;
import com.spulido.tfg.domain.plan.model.Plan;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Document(collection = "templates")
@CompoundIndex(name = "template_scope_idx", def = "{ 'organizationId': 1, 'projectId': 1 }")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Template extends BaseEntity implements ScopedEntity {

    @Field
    private String name;

    @Field
    private String description;

    @Field
    private Plan plan;

    @Field
    private String organizationId;

    @Field
    private String projectId;
}
