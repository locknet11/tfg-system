package com.spulido.agent.domain.task;

public class ServiceInfo {

    private final String name;
    private final String version;
    private final int port;

    public ServiceInfo(String name, String version, int port) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("service name must not be blank");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("service version must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        this.name = name;
        this.version = version;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public int getPort() {
        return port;
    }
}
