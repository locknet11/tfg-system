package com.spulido.tfg.domain.organization.model;

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

@Document(collection = "organizations")
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public class Organization extends BaseEntity {

    @Field
    @Indexed(unique = true)
    private String name;

    @Field
    private String description;

    @Field
    private String ownerId;

    @Field
    private List<String> memberIds = new ArrayList<>();

    @Field
    private List<String> projectIds = new ArrayList<>();

}