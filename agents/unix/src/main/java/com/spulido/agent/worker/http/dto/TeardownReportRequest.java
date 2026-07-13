package com.spulido.agent.worker.http.dto;

import java.util.List;

/**
 * Body of the teardown-outcome report sent to central before the agent exits.
 */
public class TeardownReportRequest {

    private String agentId;
    private String trigger;
    private String timestamp;
    private List<ArtifactResult> results;
    private String binaryRemoval;

    public TeardownReportRequest() {}

    public TeardownReportRequest(String agentId, String trigger, String timestamp,
                                 List<ArtifactResult> results, String binaryRemoval) {
        this.agentId = agentId;
        this.trigger = trigger;
        this.timestamp = timestamp;
        this.results = results;
        this.binaryRemoval = binaryRemoval;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<ArtifactResult> getResults() {
        return results;
    }

    public void setResults(List<ArtifactResult> results) {
        this.results = results;
    }

    public String getBinaryRemoval() {
        return binaryRemoval;
    }

    public void setBinaryRemoval(String binaryRemoval) {
        this.binaryRemoval = binaryRemoval;
    }

    /**
     * A single artifact's removal outcome.
     */
    public static class ArtifactResult {

        private String type;
        private String path;
        private String status;
        private String detail;

        public ArtifactResult() {}

        public ArtifactResult(String type, String path, String status, String detail) {
            this.type = type;
            this.path = path;
            this.status = status;
            this.detail = detail;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }
}
