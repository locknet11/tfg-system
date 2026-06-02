package com.spulido.agent.worker.http.dto;

public class ReplicationRequestBody {

    private String targetIp;
    private int targetPort;
    private String exploitId;
    private String cveId;
    private String serviceName;
    private String serviceVersion;
    private String severity;

    public ReplicationRequestBody() {}

    public String getTargetIp() { return targetIp; }
    public void setTargetIp(String targetIp) { this.targetIp = targetIp; }
    public int getTargetPort() { return targetPort; }
    public void setTargetPort(int targetPort) { this.targetPort = targetPort; }
    public String getExploitId() { return exploitId; }
    public void setExploitId(String exploitId) { this.exploitId = exploitId; }
    public String getCveId() { return cveId; }
    public void setCveId(String cveId) { this.cveId = cveId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getServiceVersion() { return serviceVersion; }
    public void setServiceVersion(String serviceVersion) { this.serviceVersion = serviceVersion; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
