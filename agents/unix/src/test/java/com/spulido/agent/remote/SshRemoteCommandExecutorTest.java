package com.spulido.agent.remote;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.domain.task.TaskResult;

class SshRemoteCommandExecutorTest {

    private SshRemoteCommandExecutor executor;
    private TargetSession session;

    @BeforeEach
    void setUp() {
        AgentConfig config = new AgentConfig();
        config.setExploitTransferFileMaxSizeMb(100);
        executor = new SshRemoteCommandExecutor(config);
        session = new TargetSession("172.31.128.4", "root", null);
    }

    @Test
    void executeSuccessPath() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("OK\n".getBytes()));

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.start()).thenReturn(mockProcess);
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                })) {

            TaskResult result = executor.execute(session, "echo OK", 10);
            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("OK"));
        }
    }

    @Test
    void executeTimeout() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(false);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.start()).thenReturn(mockProcess);
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                })) {

            TaskResult result = executor.execute(session, "sleep 999", 5);
            assertFalse(result.isSuccess());
            assertTrue(result.getFailureReason().contains("Timed out"));
        }
    }

    @Test
    void executeNonZeroExit() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(1);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("command not found\n".getBytes()));

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.start()).thenReturn(mockProcess);
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                })) {

            TaskResult result = executor.execute(session, "nonexistent", 10);
            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("exit code 1"));
        }
    }

    @Test
    void transferFileScpSuccess(@TempDir Path tempDir) throws Exception {
        byte[] content = "#!/bin/bash\necho hello".getBytes();
        Process mockScpProcess = mock(Process.class);
        when(mockScpProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockScpProcess.exitValue()).thenReturn(0);

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.start()).thenReturn(mockScpProcess);
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                })) {

            TaskResult result = executor.transferFile(session, content, "/tmp/agent", "755");
            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("SCP"));
        }
    }

    @Test
    void transferFileScpFailBase64Fallback() throws Exception {
        byte[] content = "#!/bin/bash\necho hello".getBytes();

        Process mockFail = mock(Process.class);
        when(mockFail.waitFor(anyLong(), any())).thenReturn(true);
        when(mockFail.exitValue()).thenReturn(1);

        Process mockSshOk = mock(Process.class);
        when(mockSshOk.waitFor(anyLong(), any())).thenReturn(true);
        when(mockSshOk.exitValue()).thenReturn(0);
        when(mockSshOk.getInputStream()).thenReturn(new ByteArrayInputStream("OK\n".getBytes()));

        java.util.concurrent.atomic.AtomicInteger constructionCount = new java.util.concurrent.atomic.AtomicInteger(0);

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    int idx = constructionCount.getAndIncrement();
                    if (idx < 2) {
                        when(mock.start()).thenReturn(mockFail);
                    } else {
                        when(mock.start()).thenReturn(mockSshOk);
                    }
                })) {

            TaskResult result = executor.transferFile(session, content, "/tmp/agent", "755");
            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("base64 pipe"));
        }
    }

    @Test
    void transferFileBinaryTooLarge() {
        AgentConfig config = new AgentConfig();
        config.setExploitTransferFileMaxSizeMb(1);
        SshRemoteCommandExecutor limited = new SshRemoteCommandExecutor(config);
        byte[] largeContent = new byte[2 * 1024 * 1024];

        TaskResult result = limited.transferFile(session, largeContent, "/tmp/agent", "755");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("too large"));
    }
}
