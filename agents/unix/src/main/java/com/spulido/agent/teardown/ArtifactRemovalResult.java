package com.spulido.agent.teardown;

/**
 * Per-artifact result of a teardown removal attempt.
 */
public class ArtifactRemovalResult {

    private final ArtifactType type;
    private final String path;
    private final RemovalStatus status;
    private final String detail;

    public ArtifactRemovalResult(ArtifactType type, String path, RemovalStatus status, String detail) {
        this.type = type;
        this.path = path;
        this.status = status;
        this.detail = detail != null ? detail : "";
    }

    public ArtifactType getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public RemovalStatus getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }
}
