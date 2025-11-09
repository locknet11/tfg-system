package com.spulido.tfg.domain.target.model;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.spulido.tfg.domain.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Document(collection = "targets")
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public class Target extends BaseEntity {

    @Field
    private String systemName;

    @Field
    private String description;

    @Field
    private OperatingSystem os;

    @Field
    private String uniqueId;

    @Field
    private String projectId;

    @Field
    private String ipOrDomain;

    @Field
    private TargetStatus status;

    @Field
    private String assignedAgent;

}