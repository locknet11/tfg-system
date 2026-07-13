package com.spulido.agent.teardown;

import java.util.Collections;
import java.util.List;

/**
 * Aggregate outcome of a self-destruction event, reported to central for audit
 * before the agent process exits.
 */
public class TeardownOutcome {

    /** Marker used when the agent binary is removed by the detached cleanup after exit. */
    public static final String BINARY_PENDING_DETACHED = "PENDING_DETACHED";

    private final String agentId;
    private final TeardownTrigger trigger;
    private final String timestamp;
    private final List<ArtifactRemovalResult> results;
    private final String binaryRemoval;

    public TeardownOutcome(String agentId, TeardownTrigger trigger, String timestamp,
                           List<ArtifactRemovalResult> results, String binaryRemoval) {
        this.agentId = agentId;
        this.trigger = trigger;
        this.timestamp = timestamp;
        this.results = results != null ? Collections.unmodifiableList(results) : List.of();
        this.binaryRemoval = binaryRemoval;
    }

    public String getAgentId() {
        return agentId;
    }

    public TeardownTrigger getTrigger() {
        return trigger;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<ArtifactRemovalResult> getResults() {
        return results;
    }

    public String getBinaryRemoval() {
        return binaryRemoval;
    }
}
