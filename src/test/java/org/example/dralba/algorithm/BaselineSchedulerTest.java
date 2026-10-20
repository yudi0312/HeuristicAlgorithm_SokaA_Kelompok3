package org.example.dralba.algorithm;

import org.example.dralba.model.Task;
import org.example.dralba.model.WorkerNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BaselineSchedulerTest {

    private List<WorkerNode> workers;

    @BeforeEach
    void setUp() {
        workers = List.of(
                new WorkerNode("w1", "Worker-1", "http://localhost:8081", 1000.0, 1),
                new WorkerNode("w2", "Worker-2", "http://localhost:8082", 2000.0, 1),
                new WorkerNode("w3", "Worker-3", "http://localhost:8083", 3000.0, 2)
        );
    }

    @Test
    @DisplayName("Round Robin harus membagi task secara bergilir (siklis)")
    void testRoundRobinDistribution() {
        RoundRobinScheduler rr = new RoundRobinScheduler();
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            tasks.add(new Task(i, 1000));
        }

        Map<WorkerNode, List<Task>> allocation = rr.schedule(tasks, workers);

        assertEquals(3, allocation.get(workers.get(0)).size());
        assertEquals(3, allocation.get(workers.get(1)).size());
        assertEquals(3, allocation.get(workers.get(2)).size());

        // Verifikasi task 0 ke w1, task 1 ke w2, task 2 ke w3, task 3 ke w1
        assertEquals(0, allocation.get(workers.get(0)).get(0).getId());
        assertEquals(3, allocation.get(workers.get(0)).get(1).getId());
        assertEquals(6, allocation.get(workers.get(0)).get(2).getId());

        assertEquals(1, allocation.get(workers.get(1)).get(0).getId());
        assertEquals(2, allocation.get(workers.get(2)).get(0).getId());
    }

    @Test
    @DisplayName("FCFS harus mengalokasikan task ke worker dengan estimasi waktu selesai paling awal")
    void testFCFSDistribution() {
        FCFSScheduler fcfs = new FCFSScheduler();
        List<Task> tasks = List.of(
                new Task(1, 10000),
                new Task(2, 10000),
                new Task(3, 10000),
                new Task(4, 10000)
        );

        Map<WorkerNode, List<Task>> allocation = fcfs.schedule(tasks, workers);

        int totalAllocated = allocation.values().stream().mapToInt(List::size).sum();
        assertEquals(4, totalAllocated);

        // Worker dengan MIPS lebih tinggi (w3 = 3000) menyelesaikan task lebih cepat sehingga dapat mengambil task berikutnya
        assertTrue(allocation.get(workers.get(2)).size() >= allocation.get(workers.get(0)).size());
    }
}

