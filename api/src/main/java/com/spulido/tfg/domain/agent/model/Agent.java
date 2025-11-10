package com.spulido.tfg.domain.agent.model;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.spulido.tfg.domain.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Document(collection = "agents")
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public class Agent extends BaseEntity {

    @Field
    private String name;

    @Field
    private AgentStatus status;

    @Field
    private String version;

    @Field
    private LocalDateTime lastConnection;
}
