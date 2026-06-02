package com.spulido.agent.remote;

public class TargetSession {

    private final String targetIp;
    private final String targetUser;
    private final String sshIdentityFile;

    public TargetSession(String targetIp, String targetUser, String sshIdentityFile) {
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
}
