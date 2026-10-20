package org.example.dralba.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Menyimpan seluruh metrik evaluasi eksperimen penjadwalan.
 */
public class EvaluationMetrics implements Serializable {
    private final String algorithmName;
    private final String datasetName;
    private final int totalTasks;
    private final int successfulTasks;
    private final int failedTasks;
    private final double makespanSeconds;
    private final double throughputTasksPerSec;
    private final double averageResponseTimeMs;
    private final double averageWaitingTimeMs;
    private final double averageResourceUtilizationPercent; // ARUR
    private final double loadBalancingStdDevMs;
    private final double schedulingOverheadMs;
    private final Map<String, WorkerMetricDetail> workerDetails;

    public EvaluationMetrics(String algorithmName, String datasetName,
                             int totalTasks, int successfulTasks, int failedTasks,
                             double makespanSeconds, double throughputTasksPerSec,
                             double averageResponseTimeMs, double averageWaitingTimeMs,
                             double averageResourceUtilizationPercent,
                             double loadBalancingStdDevMs, double schedulingOverheadMs,
                             Map<String, WorkerMetricDetail> workerDetails) {
        this.algorithmName = algorithmName;
        this.datasetName = datasetName;
        this.totalTasks = totalTasks;
        this.successfulTasks = successfulTasks;
        this.failedTasks = failedTasks;
        this.makespanSeconds = makespanSeconds;
        this.throughputTasksPerSec = throughputTasksPerSec;
        this.averageResponseTimeMs = averageResponseTimeMs;
        this.averageWaitingTimeMs = averageWaitingTimeMs;
        this.averageResourceUtilizationPercent = averageResourceUtilizationPercent;
        this.loadBalancingStdDevMs = loadBalancingStdDevMs;
        this.schedulingOverheadMs = schedulingOverheadMs;
        this.workerDetails = workerDetails != null ? workerDetails : Collections.emptyMap();
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getSuccessfulTasks() {
        return successfulTasks;
    }

    public int getFailedTasks() {
        return failedTasks;
    }

    public double getMakespanSeconds() {
        return makespanSeconds;
    }

    public double getThroughputTasksPerSec() {
        return throughputTasksPerSec;
    }

    public double getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    public double getAverageWaitingTimeMs() {
        return averageWaitingTimeMs;
    }

    public double getAverageResourceUtilizationPercent() {
        return averageResourceUtilizationPercent;
    }

    public double getLoadBalancingStdDevMs() {
        return loadBalancingStdDevMs;
    }

    public double getSchedulingOverheadMs() {
        return schedulingOverheadMs;
    }

    public Map<String, WorkerMetricDetail> getWorkerDetails() {
        return workerDetails;
    }

    public static class WorkerMetricDetail implements Serializable {
        private final String workerId;
        private final int tasksExecuted;
        private final double totalBusyTimeMs;
        private final double utilizationPercent;

        public WorkerMetricDetail(String workerId, int tasksExecuted, double totalBusyTimeMs, double utilizationPercent) {
            this.workerId = workerId;
            this.tasksExecuted = tasksExecuted;
            this.totalBusyTimeMs = totalBusyTimeMs;
            this.utilizationPercent = utilizationPercent;
        }

        public String getWorkerId() {
            return workerId;
        }

        public int getTasksExecuted() {
            return tasksExecuted;
        }

        public double getTotalBusyTimeMs() {
            return totalBusyTimeMs;
        }

        public double getUtilizationPercent() {
            return utilizationPercent;
        }
    }
}

