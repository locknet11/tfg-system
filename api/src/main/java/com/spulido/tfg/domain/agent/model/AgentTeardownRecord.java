package com.spulido.tfg.domain.agent.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.spulido.tfg.domain.BaseEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Audit record of an agent self-destruction event, persisted for traceability.
 */
@Document(collection = "agent_teardown_records")
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public class AgentTeardownRecord extends BaseEntity {

    @Field
    @Indexed
    private String agentId;

    @Field
    private String organizationId;

    @Field
    private String projectId;

    @Field
    private String trigger;

    @Field
    private Instant reportedAt;

    @Field
    private String agentTimestamp;

    @Field
    private List<ArtifactResult> results;

    @Field
    private String binaryRemoval;

    @Getter
    @Setter
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class ArtifactResult {

        @Field
        private String type;

        @Field
        private String path;

        @Field
        private String status;

        @Field
        private String detail;
    }
}
