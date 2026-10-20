package org.example.dralba.scheduler;

import org.example.dralba.model.EvaluationMetrics;
import org.example.dralba.model.TaskExecutionResult;
import org.example.dralba.model.WorkerNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MetricsCalculatorTest {

    @Test
    @DisplayName("Formula metrik Makespan, Throughput, ART, dan ARUR harus tepat")
    void testMetricsCalculation() {
        List<WorkerNode> workers = List.of(
                new WorkerNode("w1", "Worker-1", "http://localhost:8081", 1000.0, 1),
                new WorkerNode("w2", "Worker-2", "http://localhost:8082", 2000.0, 1)
        );

        // Simulasi hasil 2 task:
        // Task 1 di w1: submit 0, start 0, finish 1000 ms (durasi 1000ms)
        // Task 2 di w2: submit 0, start 0, finish 500 ms (durasi 500ms)
        TaskExecutionResult r1 = new TaskExecutionResult(
                1, "w1", 1000, "SUCCESS",
                0, 0, 1000, 1000.0, 950.0, "abc", null
        );
        TaskExecutionResult r2 = new TaskExecutionResult(
                2, "w2", 1000, "SUCCESS",
                0, 0, 500, 500.0, 480.0, "def", null
        );

        List<TaskExecutionResult> results = List.of(r1, r2);

        EvaluationMetrics metrics = MetricsCalculator.calculate(
                "TEST_ALGO", "TEST_DATASET",
                results, workers, 5.0, 0, 1000
        );

        // Makespan = max(1000, 500) - min(0, 0) = 1000 ms = 1.0 detik
        assertEquals(1.0, metrics.getMakespanSeconds(), 0.001);

        // Throughput = 2 tasks / 1.0 detik = 2.0 task/detik
        assertEquals(2.0, metrics.getThroughputTasksPerSec(), 0.001);

        // ART = (1000 + 500) / 2 = 750.0 ms
        assertEquals(750.0, metrics.getAverageResponseTimeMs(), 0.001);

        // Total busy time = 1000 + 500 = 1500 ms
        // ARUR = 1500 / (2 * 1000) * 100% = 75.0%
        assertEquals(75.0, metrics.getAverageResourceUtilizationPercent(), 0.01);

        // Load Balancing StdDev:
        // w1 busy = 1000, w2 busy = 500, mean = 750
        // diff1 = 250^2 = 62500, diff2 = (-250)^2 = 62500, mean diff = 62500, sqrt = 250.0
        assertEquals(250.0, metrics.getLoadBalancingStdDevMs(), 0.01);
    }
}

