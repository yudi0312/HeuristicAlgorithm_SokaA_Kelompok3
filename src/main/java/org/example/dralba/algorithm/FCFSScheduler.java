package org.example.dralba.algorithm;

import org.example.dralba.model.Task;
import org.example.dralba.model.WorkerNode;

import java.util.*;

/**
 * Baseline FCFS (First-Come First-Served) Scheduler.
 * Task dialokasikan sesuai urutan kedatangan (urutan dataset asli) ke worker
 * yang memiliki estimasi waktu penyelesaian (EFT) paling awal saat task tiba.
 */
public class FCFSScheduler implements TaskScheduler {

    @Override
    public String getName() {
        return "FCFS";
    }

    @Override
    public Map<WorkerNode, List<Task>> schedule(List<Task> tasks, List<WorkerNode> workers) {
        if (workers == null || workers.isEmpty()) {
            throw new IllegalArgumentException("Daftar worker tidak boleh kosong.");
        }

        Map<WorkerNode, List<Task>> allocation = new LinkedHashMap<>();
        Map<WorkerNode, Double> workerEftMap = new HashMap<>();

        for (WorkerNode worker : workers) {
            allocation.put(worker, new ArrayList<>());
            workerEftMap.put(worker, 0.0);
        }

        if (tasks == null || tasks.isEmpty()) {
            return allocation;
        }

        // FCFS mempertahankan urutan kedatangan task (tanpa pengurutan ulang berdasarkan panjang)
        for (Task task : tasks) {
            WorkerNode earliestWorker = workerEftMap.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(workers.get(0));

            allocation.get(earliestWorker).add(task);
            double execTime = task.getLength() / earliestWorker.getCapacityMips();
            workerEftMap.put(earliestWorker, workerEftMap.get(earliestWorker) + execTime);
        }

        return allocation;
    }
}

