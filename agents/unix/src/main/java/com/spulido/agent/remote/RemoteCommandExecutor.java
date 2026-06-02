package com.spulido.agent.remote;

import com.spulido.agent.domain.task.TaskResult;

public interface RemoteCommandExecutor {

    TaskResult execute(TargetSession session, String command, long timeoutSeconds);

    TaskResult transferFile(TargetSession session, byte[] content, String remotePath, String permissions);
}
