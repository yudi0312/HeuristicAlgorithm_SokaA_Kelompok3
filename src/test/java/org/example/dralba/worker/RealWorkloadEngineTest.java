package org.example.dralba.worker;

import org.example.dralba.model.Task;
import org.example.dralba.model.TaskExecutionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RealWorkloadEngineTest {

    @Test
    @DisplayName("Workload engine harus mengeksekusi komputasi nyata dan menghasilkan checksum SHA-256")
    void testRealWorkloadExecution() {
        RealWorkloadEngine engine = new RealWorkloadEngine(2);
        Task task = new Task(101, 5000);

        TaskExecutionResult result = engine.execute(task, "test-worker");

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(101, result.getTaskId());
        assertEquals("test-worker", result.getWorkerId());
        assertTrue(result.getExecutionTimeMs() > 0, "Execution time harus lebih dari 0ms");
        assertNotNull(result.getChecksum(), "Checksum tidak boleh null");
        assertEquals(64, result.getChecksum().length(), "SHA-256 hex string harus berpanjang 64 karakter");
    }

    @Test
    @DisplayName("Task yang sama harus menghasilkan checksum identik (deterministik)")
    void testDeterministicExecution() {
        RealWorkloadEngine engine = new RealWorkloadEngine(2);
        Task task = new Task(202, 10000);

        TaskExecutionResult res1 = engine.execute(task, "w1");
        TaskExecutionResult res2 = engine.execute(task, "w2");

        assertEquals(res1.getChecksum(), res2.getChecksum(), "Checksum harus deterministik untuk input task yang sama");
    }
}

