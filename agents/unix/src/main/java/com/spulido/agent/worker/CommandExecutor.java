package com.spulido.agent.worker;

import com.spulido.agent.domain.task.TaskResult;

public interface CommandExecutor {

    TaskResult execute(String command, long timeoutSeconds);
}
