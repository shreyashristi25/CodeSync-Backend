package com.codesync.execution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesync.execution.entity.ExecutionLanguage;
import com.codesync.execution.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DockerExecutionServiceTest {

    private DockerExecutionService createService() {
        DockerExecutionService svc = new DockerExecutionService();
        ReflectionTestUtils.setField(svc, "timeoutSeconds", 10);
        ReflectionTestUtils.setField(svc, "pythonCommand", "python");
        ReflectionTestUtils.setField(svc, "nodeCommand", "node");
        ReflectionTestUtils.setField(svc, "javaCommand", "java");
        return svc;
    }

    @Test
    void shouldRejectBlankCode() {
        DockerExecutionService svc = createService();
        assertThrows(BadRequestException.class, () -> svc.runInEphemeralEnvironment("job1", "  ", ExecutionLanguage.PYTHON, null));
    }

    @Test
    void shouldRejectNullCode() {
        DockerExecutionService svc = createService();
        assertThrows(BadRequestException.class, () -> svc.runInEphemeralEnvironment("job1", null, ExecutionLanguage.PYTHON, null));
    }

    @Test
    void shouldRejectEmptyCode() {
        DockerExecutionService svc = createService();
        assertThrows(BadRequestException.class, () -> svc.runInEphemeralEnvironment("job1", "", ExecutionLanguage.PYTHON, null));
    }

    @Test
    void shouldCreateResultWithExitCode() {
        DockerExecutionService.ExecutionRunResult result = new DockerExecutionService.ExecutionRunResult(0, "output", "error");
        assertEquals(0, result.exitCode());
        assertEquals("output", result.stdout());
        assertEquals("error", result.stderr());
    }

    @Test
    void shouldHandleCancelJobWhenNoProcess() {
        DockerExecutionService svc = createService();
        svc.cancelJob("nonexistent-job");
    }
}
