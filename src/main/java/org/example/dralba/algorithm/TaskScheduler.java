package org.example.dralba.algorithm;

import org.example.dralba.model.Task;
import org.example.dralba.model.WorkerNode;

import java.util.List;
import java.util.Map;

/**
 * Interface dasar untuk algoritma penjadwalan task ke worker node.
 */
public interface TaskScheduler {
    /**
     * Mengembalikan nama algoritma.
     */
    String getName();

    /**
     * Memetakan daftar task ke daftar worker node heterogen.
     *
     * @param tasks   daftar task yang akan dijadwalkan
     * @param workers daftar worker node heterogen
     * @return mapping worker node ke daftar task yang dialokasikan
     */
    Map<WorkerNode, List<Task>> schedule(List<Task> tasks, List<WorkerNode> workers);
}

