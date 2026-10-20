package org.example.dralba.scheduler;

import org.example.dralba.model.EvaluationMetrics;
import org.example.dralba.model.TaskExecutionResult;
import org.example.dralba.model.WorkerNode;

import java.util.*;

/**
 * Kalkulator metrik evaluasi penjadwalan sesuai standar komputasi awan.
 * Metrik yang dihitung:
 * - Makespan (detik)
 * - Throughput (task/detik)
 * - Average Response Time (ART) (ms)
 * - Average Resource Utilization (ARUR) (%)
 * - Load Balancing / Degree of Imbalance (StdDev waktu kerja antar worker) (ms)
 */
public class MetricsCalculator {

    public static EvaluationMetrics calculate(String algorithmName, String datasetName,
                                              List<TaskExecutionResult> results,
                                              List<WorkerNode> workers,
                                              double schedulingOverheadMs,
                                              long globalStartMs,
                                              long globalFinishMs) {
        if (results == null || results.isEmpty()) {
            return new EvaluationMetrics(algorithmName, datasetName, 0, 0, 0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, schedulingOverheadMs, Collections.emptyMap());
        }

        int totalTasks = results.size();
        int successfulTasks = 0;
        int failedTasks = 0;

        double sumResponseTime = 0.0;
        double sumWaitingTime = 0.0;

        // Hitung waktu kerja (busy time) per worker
        Map<String, Double> workerBusyTimes = new LinkedHashMap<>();
        Map<String, Integer> workerTaskCounts = new LinkedHashMap<>();
        for (WorkerNode w : workers) {
            workerBusyTimes.put(w.getId(), 0.0);
            workerTaskCounts.put(w.getId(), 0);
        }

        for (TaskExecutionResult res : results) {
            if ("SUCCESS".equalsIgnoreCase(res.getStatus())) {
                successfulTasks++;
            } else {
                failedTasks++;
            }

            sumResponseTime += res.getResponseTimeMs();
            sumWaitingTime += res.getWaitingTimeMs();

            String wid = res.getWorkerId();
            workerBusyTimes.put(wid, workerBusyTimes.getOrDefault(wid, 0.0) + res.getExecutionTimeMs());
            workerTaskCounts.put(wid, workerTaskCounts.getOrDefault(wid, 0) + 1);
        }

        // Makespan dihitung dari wall-clock waktu mulai pengiriman task pertama
        // hingga selesai eksekusi task terakhir
        long minSubmit = results.stream().mapToLong(TaskExecutionResult::getSubmitTimeMs).min().orElse(globalStartMs);
        long maxFinish = results.stream().mapToLong(TaskExecutionResult::getFinishTimeMs).max().orElse(globalFinishMs);
        double makespanMs = Math.max(1.0, (double) (maxFinish - minSubmit));
        double makespanSeconds = makespanMs / 1000.0;

        double throughput = (makespanSeconds > 0) ? (successfulTasks / makespanSeconds) : 0.0;
        double avgResponseTime = sumResponseTime / totalTasks;
        double avgWaitingTime = sumWaitingTime / totalTasks;

        // ARUR (Average Resource Utilization Ratio):
        // Total Busy Time seluruh worker / (Jumlah Worker * Makespan) * 100%
        int workerCount = Math.max(1, workers.size());
        double totalWorkerBusyTime = workerBusyTimes.values().stream().mapToDouble(Double::doubleValue).sum();
        double arurPercent = (totalWorkerBusyTime / (workerCount * makespanMs)) * 100.0;
        if (arurPercent > 100.0) {
            arurPercent = 100.0; // Normalisasi batas atas jika ada concurrency multi-core
        }

        // Load Balancing: Standar Deviasi busy time antar worker
        double avgBusyTime = totalWorkerBusyTime / workerCount;
        double sumSquaredDiff = 0.0;
        for (double busyTime : workerBusyTimes.values()) {
            sumSquaredDiff += Math.pow(busyTime - avgBusyTime, 2);
        }
        double loadBalancingStdDevMs = Math.sqrt(sumSquaredDiff / workerCount);

        // Detail per-worker
        Map<String, EvaluationMetrics.WorkerMetricDetail> workerDetails = new LinkedHashMap<>();
        for (WorkerNode w : workers) {
            String wid = w.getId();
            double busy = workerBusyTimes.getOrDefault(wid, 0.0);
            int count = workerTaskCounts.getOrDefault(wid, 0);
            double util = (makespanMs > 0) ? (busy / makespanMs) * 100.0 : 0.0;
            workerDetails.put(wid, new EvaluationMetrics.WorkerMetricDetail(wid, count, busy, util));
        }

        return new EvaluationMetrics(
                algorithmName,
                datasetName,
                totalTasks,
                successfulTasks,
                failedTasks,
                makespanSeconds,
                throughput,
                avgResponseTime,
                avgWaitingTime,
                arurPercent,
                loadBalancingStdDevMs,
                schedulingOverheadMs,
                workerDetails
        );
    }
}

