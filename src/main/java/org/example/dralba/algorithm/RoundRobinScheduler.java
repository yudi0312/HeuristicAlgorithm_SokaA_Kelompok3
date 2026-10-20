package org.example.dralba.algorithm;

import org.example.dralba.model.Task;
import org.example.dralba.model.WorkerNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Baseline Round Robin (RR) Scheduler.
 * Task dialokasikan secara bergilir (siklis) ke setiap worker tanpa memperhitungkan
 * panjang task maupun heterogenitas kapasitas pemrosesan worker.
 */
public class RoundRobinScheduler implements TaskScheduler {

    @Override
    public String getName() {
        return "RoundRobin";
    }

    @Override
    public Map<WorkerNode, List<Task>> schedule(List<Task> tasks, List<WorkerNode> workers) {
        if (workers == null || workers.isEmpty()) {
            throw new IllegalArgumentException("Daftar worker tidak boleh kosong.");
        }

        Map<WorkerNode, List<Task>> allocation = new LinkedHashMap<>();
        for (WorkerNode worker : workers) {
            allocation.put(worker, new ArrayList<>());
        }

        if (tasks == null || tasks.isEmpty()) {
            return allocation;
        }

        int workerCount = workers.size();
        for (int i = 0; i < tasks.size(); i++) {
            WorkerNode targetWorker = workers.get(i % workerCount);
            allocation.get(targetWorker).add(tasks.get(i));
        }

        return allocation;
    }
}

