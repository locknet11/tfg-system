package com.spulido.agent.remote;

import com.spulido.agent.domain.task.TaskResult;

/**
 * Dispatches to the {@link RemoteCommandExecutor} matching the session's access
 * channel (SSH vs. an exploited Docker API), so step handlers can work against a
 * {@link TargetSession} without knowing which channel produced it.
 */
public class CompositeRemoteCommandExecutor implements RemoteCommandExecutor {

    private final RemoteCommandExecutor sshExecutor;
    private final RemoteCommandExecutor dockerApiExecutor;

    public CompositeRemoteCommandExecutor(RemoteCommandExecutor sshExecutor, RemoteCommandExecutor dockerApiExecutor) {
        this.sshExecutor = sshExecutor;
        this.dockerApiExecutor = dockerApiExecutor;
    }

    @Override
    public TaskResult execute(TargetSession session, String command, long timeoutSeconds) {
        return delegateFor(session).execute(session, command, timeoutSeconds);
    }

    @Override
    public TaskResult transferFile(TargetSession session, byte[] content, String remotePath, String permissions) {
        return delegateFor(session).transferFile(session, content, remotePath, permissions);
    }

    private RemoteCommandExecutor delegateFor(TargetSession session) {
        return session.getChannelType() == TargetSession.ChannelType.DOCKER_API ? dockerApiExecutor : sshExecutor;
    }
}
