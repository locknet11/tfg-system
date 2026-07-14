package com.spulido.agent.remote;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class DockerApiRemoteCommandExecutorTest {

    private static final String TARGET_IP = "127.0.0.1";
    private static final String CONTAINER_NAME = "tfg-repl-127-0-0-1";

    private HttpServer server;
    private int port;
    private final AtomicReference<String> lastExecCmd = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(TARGET_IP, 0), 0);
        port = server.getAddress().getPort();

        // Container does not exist yet -> 404 on inspect, then create + start succeed.
        server.createContext("/containers/" + CONTAINER_NAME + "/json", ex -> respond(ex, 404, "{}"));
        server.createContext("/images/create", ex -> respond(ex, 200, ""));
        server.createContext("/containers/create", ex -> respond(ex, 201, "{\"Id\":\"abc123\"}"));
        server.createContext("/containers/" + CONTAINER_NAME + "/start", ex -> respond(ex, 204, ""));
        server.createContext("/containers/" + CONTAINER_NAME + "/exec", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes());
            lastExecCmd.set(body);
            respond(ex, 201, "{\"Id\":\"exec-1\"}");
        });
        server.createContext("/exec/exec-1/start", ex -> respond(ex, 200, "root\nlab-vm\n"));
        server.createContext("/exec/exec-1/json", ex -> respond(ex, 200, "{\"ExitCode\":0}"));
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange ex, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes();
        ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            ex.getResponseBody().write(bytes);
        }
        ex.close();
    }

    @Test
    void executesCommandThroughChrootOnHostFilesystem() {
        DockerApiRemoteCommandExecutor executor = new DockerApiRemoteCommandExecutor();
        TargetSession session = TargetSession.dockerApi(TARGET_IP, port);

        var result = executor.execute(session, "id && hostname", 10);

        assertTrue(result.isSuccess());
        assertTrue(lastExecCmd.get().contains("chroot"));
        assertTrue(lastExecCmd.get().contains("/mnt"));
    }

    @Test
    void refusesToPushOversizedPayload() {
        DockerApiRemoteCommandExecutor executor = new DockerApiRemoteCommandExecutor();
        TargetSession session = TargetSession.dockerApi(TARGET_IP, port);

        byte[] huge = new byte[3_000_000];
        var result = executor.transferFile(session, huge, "/tmp/agent", "755");

        assertFalse(result.isSuccess());
        assertTrue(result.getFailureReason().toLowerCase().contains("large"));
    }

    @Test
    void transfersSmallScriptViaBase64Exec() {
        DockerApiRemoteCommandExecutor executor = new DockerApiRemoteCommandExecutor();
        TargetSession session = TargetSession.dockerApi(TARGET_IP, port);

        var result = executor.transferFile(session, "#!/bin/sh\necho hi".getBytes(), "/tmp/install.sh", "755");

        assertTrue(result.isSuccess());
        assertTrue(lastExecCmd.get().contains("base64 -d"));
    }
}
