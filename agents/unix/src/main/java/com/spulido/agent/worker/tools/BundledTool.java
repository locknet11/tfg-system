package com.spulido.agent.worker.tools;

/**
 * Identifies a bundled capability by role rather than by product name.
 * Each constant holds the backing binary name and a human-readable
 * capability description used in log/error messages.
 */
public enum BundledTool {

    NETWORK_DISCOVERY("nmap", "Network host discovery"),
    PORT_SERVICE_SCAN("rustscan", "Port & service scan"),
    RAW_TCP("nc", "Raw TCP connectivity"),
    FILE_RETRIEVAL("curl", "Remote file retrieval");

    private final String binaryName;
    private final String capabilityDescription;

    BundledTool(String binaryName, String capabilityDescription) {
        this.binaryName = binaryName;
        this.capabilityDescription = capabilityDescription;
    }

    public String getBinaryName() {
        return binaryName;
    }

    public String getCapabilityDescription() {
        return capabilityDescription;
    }
}
