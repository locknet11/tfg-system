package com.spulido.agent.remote;

public class TargetSession {

    public enum ChannelType {
        SSH,
        DOCKER_API
    }

    private final String targetIp;
    private final String targetUser;
    private final String sshIdentityFile;
    private final ChannelType channelType;
    private final int dockerApiPort;

    public TargetSession(String targetIp, String targetUser, String sshIdentityFile) {
        this(targetIp, targetUser, sshIdentityFile, ChannelType.SSH, 0);
    }

    private TargetSession(String targetIp, String targetUser, String sshIdentityFile,
                          ChannelType channelType, int dockerApiPort) {
        if (targetIp == null || targetIp.isBlank()) {
            throw new IllegalArgumentException("targetIp must not be blank");
        }
        if (targetUser == null || targetUser.isBlank()) {
            throw new IllegalArgumentException("targetUser must not be blank");
        }
        this.targetIp = targetIp;
        this.targetUser = targetUser;
        this.sshIdentityFile = (sshIdentityFile != null && !sshIdentityFile.isBlank())
                ? sshIdentityFile : null;
        this.channelType = channelType;
        this.dockerApiPort = dockerApiPort;
    }

    /**
     * A session reached through an unauthenticated Docker Engine API on the target,
     * rather than SSH. No credentials are required — the channel itself is the exploit.
     */
    public static TargetSession dockerApi(String targetIp, int dockerApiPort) {
        return new TargetSession(targetIp, "root", null, ChannelType.DOCKER_API, dockerApiPort);
    }

    public String getTargetIp() {
        return targetIp;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public String getSshIdentityFile() {
        return sshIdentityFile;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public int getDockerApiPort() {
        return dockerApiPort;
    }
}
