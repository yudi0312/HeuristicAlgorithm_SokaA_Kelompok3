package org.example.dralba.model;

import java.io.Serializable;

/**
 * Menyimpan hasil aktual dari eksekusi task pada worker.
 */
public class TaskExecutionResult implements Serializable {
    private final int taskId;
    private final String workerId;
    private final long taskLength;
    private final String status;
    private final long submitTimeMs;
    private final long startTimeMs;
    private final long finishTimeMs;
    private final double executionTimeMs;
    private final double cpuTimeMs;
    private final String checksum;
    private final String errorMessage;

    public TaskExecutionResult(int taskId, String workerId, long taskLength, String status,
                               long submitTimeMs, long startTimeMs, long finishTimeMs,
                               double executionTimeMs, double cpuTimeMs,
                               String checksum, String errorMessage) {
        this.taskId = taskId;
        this.workerId = workerId;
        this.taskLength = taskLength;
        this.status = status;
        this.submitTimeMs = submitTimeMs;
        this.startTimeMs = startTimeMs;
        this.finishTimeMs = finishTimeMs;
        this.executionTimeMs = executionTimeMs;
        this.cpuTimeMs = cpuTimeMs;
        this.checksum = checksum;
        this.errorMessage = errorMessage;
    }

    public int getTaskId() {
        return taskId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public long getTaskLength() {
        return taskLength;
    }

    public String getStatus() {
        return status;
    }

    public long getSubmitTimeMs() {
        return submitTimeMs;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public long getFinishTimeMs() {
        return finishTimeMs;
    }

    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public double getCpuTimeMs() {
        return cpuTimeMs;
    }

    public double getResponseTimeMs() {
        return Math.max(0, finishTimeMs - submitTimeMs);
    }

    public double getWaitingTimeMs() {
        return Math.max(0, startTimeMs - submitTimeMs);
    }

    public String getChecksum() {
        return checksum;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "TaskExecutionResult{" +
                "taskId=" + taskId +
                ", workerId='" + workerId + '\'' +
                ", status='" + status + '\'' +
                ", execTime=" + String.format("%.2f", executionTimeMs) + "ms" +
                ", cpuTime=" + String.format("%.2f", cpuTimeMs) + "ms" +
                '}';
    }
}

