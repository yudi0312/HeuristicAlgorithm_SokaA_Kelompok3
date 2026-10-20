package org.example.dralba.algorithm;

import org.example.dralba.model.Task;
import org.example.dralba.model.WorkerNode;

import java.util.*;

/**
 * Implementasi algoritma DRALBA (Dynamic Resource Allocation and Load Balancing Algorithm).
 * Menggunakan pendekatan 2 fase:
 * 1. Spill Scheduler: Alokasi berdasarkan kuota share komputasi relatif per worker.
 * 2. Fill Scheduler: Alokasi sisa task ke worker dengan Expected Finish Time (EFT) minimum.
 */
public class DRALBAScheduler implements TaskScheduler {

    @Override
    public String getName() {
        return "DRALBA";
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

        double totalCapacity = workers.stream()
                .mapToDouble(WorkerNode::getCapacityMips)
                .sum();
        double totalLength = tasks.stream()
                .mapToDouble(Task::getLength)
                .sum();

        // Inisialisasi kuota share dan EFT (Expected Finish Time) per worker
        Map<WorkerNode, Double> workerShareMap = new HashMap<>();
        Map<WorkerNode, Double> workerEftMap = new HashMap<>();

        for (WorkerNode worker : workers) {
            double computRatio = worker.getCapacityMips() / totalCapacity;
            workerShareMap.put(worker, totalLength * computRatio);
            workerEftMap.put(worker, 0.0);
        }

        // Urutkan task descending berdasarkan panjang (workload)
        List<Task> unmappedTasks = new ArrayList<>(tasks);
        unmappedTasks.sort(Comparator.comparingLong(Task::getLength).reversed());

        // --- FASE 1: SPILL SCHEDULER ---
        while (!unmappedTasks.isEmpty()) {
            // Pilih worker dengan sisa share terbesar
            WorkerNode maxShareWorker = workerShareMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (maxShareWorker == null) {
                break;
            }

            double maxShare = workerShareMap.get(maxShareWorker);
            if (maxShare <= 0) {
                break; // Semua share telah terpakai
            }

            // Cari task terbesar yang masih muat dalam sisa share worker tersebut
            Task fittingTask = null;
            for (Task task : unmappedTasks) {
                if (task.getLength() <= maxShare) {
                    fittingTask = task;
                    break;
                }
            }

            if (fittingTask != null) {
                allocation.get(maxShareWorker).add(fittingTask);
                workerShareMap.put(maxShareWorker, maxShare - fittingTask.getLength());
                double taskEstTime = fittingTask.getLength() / maxShareWorker.getCapacityMips();
                workerEftMap.put(maxShareWorker, workerEftMap.get(maxShareWorker) + taskEstTime);
                unmappedTasks.remove(fittingTask);
            } else {
                // Tidak ada lagi task yang muat pada sisa share terbesar
                break;
            }
        }

        // --- FASE 2: FILL SCHEDULER ---
        // Sisa task dialokasikan ke worker dengan EFT terkecil
        for (Task task : unmappedTasks) {
            WorkerNode minEftWorker = workerEftMap.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(workers.get(0));

            allocation.get(minEftWorker).add(task);
            double taskEstTime = task.getLength() / minEftWorker.getCapacityMips();
            workerEftMap.put(minEftWorker, workerEftMap.get(minEftWorker) + taskEstTime);
        }

        return allocation;
    }
}

