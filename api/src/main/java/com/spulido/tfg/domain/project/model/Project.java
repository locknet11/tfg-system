package com.spulido.tfg.domain.project.model;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.spulido.tfg.domain.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Document(collection = "projects")
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public class Project extends BaseEntity {

    @Field
    @Indexed(unique = true)
    private String name;

    @Field
    private String description;

    @Field
    private String organizationId;

    @Field
    private ProjectStatus status = ProjectStatus.ACTIVE;

}