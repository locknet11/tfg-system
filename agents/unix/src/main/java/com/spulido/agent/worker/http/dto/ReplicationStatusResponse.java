package com.spulido.agent.worker.http.dto;

public class ReplicationStatusResponse {

    private String status;
    private String replicationToken;
    private String downloadUrl;
    private String preauthCode;
    private String centralUrl;

    public ReplicationStatusResponse() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReplicationToken() { return replicationToken; }
    public void setReplicationToken(String replicationToken) { this.replicationToken = replicationToken; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getPreauthCode() { return preauthCode; }
    public void setPreauthCode(String preauthCode) { this.preauthCode = preauthCode; }
    public String getCentralUrl() { return centralUrl; }
    public void setCentralUrl(String centralUrl) { this.centralUrl = centralUrl; }
}
