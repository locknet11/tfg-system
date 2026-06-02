package com.spulido.agent.remote;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import com.spulido.agent.config.AgentConfig;

class SshSessionProvisionerTest {

    private SshSessionProvisioner provisioner;
    private AgentConfig config;

    @BeforeEach
    void setUp() {
        config = new AgentConfig();
        config.setExploitDefaultTargetUser("root");
        provisioner = new SshSessionProvisioner(config);
    }

    @Test
    void verifySuccess() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("OK\n".getBytes()));

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.start()).thenReturn(mockProcess);
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                })) {

            assertTrue(provisioner.verify("172.31.128.4", "root", null));
        }
    }

    @Test
    void verifyFailureAfterRetries() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(255);

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.start()).thenReturn(mockProcess);
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                })) {

            assertFalse(provisioner.verify("172.31.128.4", "admin", null));
        }
    }

    @Test
    void verifyUsesDefaultUserWhenNull() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("OK\n".getBytes()));

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.start()).thenReturn(mockProcess);
                    when(mock.redirectErrorStream(true)).thenReturn(mock);
                })) {

            assertTrue(provisioner.verify("172.31.128.4", null, null));
        }
    }
}
