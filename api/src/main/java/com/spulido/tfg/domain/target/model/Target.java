package com.spulido.tfg.domain.target.model;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.spulido.tfg.domain.BaseEntity;
import com.spulido.tfg.domain.ScopedEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Document(collection = "targets")
@CompoundIndex(name = "org_proj_idx", def = "{ 'organizationId': 1, 'projectId': 1 }")
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public class Target extends BaseEntity implements ScopedEntity {

    @Field
    private String systemName;

    @Field
    private String description;

    @Field
    private OperatingSystem os;

    @Field
    private String uniqueId;

    @Field
    private String organizationId;

    @Field
    private String projectId;

    @Field
    private String ipOrDomain;

    @Field
    private TargetStatus status;

    @Field
    private String assignedAgent;

}