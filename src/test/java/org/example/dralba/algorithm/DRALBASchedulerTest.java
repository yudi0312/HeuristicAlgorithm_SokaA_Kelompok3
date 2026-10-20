package org.example.dralba.algorithm;

import org.example.dralba.model.Task;
import org.example.dralba.model.WorkerNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DRALBASchedulerTest {

    private DRALBAScheduler scheduler;
    private List<WorkerNode> workers;

    @BeforeEach
    void setUp() {
        scheduler = new DRALBAScheduler();
        workers = List.of(
                new WorkerNode("w1", "Worker-1", "http://localhost:8081", 1000.0, 1),
                new WorkerNode("w2", "Worker-2", "http://localhost:8082", 2000.0, 1),
                new WorkerNode("w3", "Worker-3", "http://localhost:8083", 3000.0, 2)
        );
    }

    @Test
    @DisplayName("Semua task harus teralokasikan tanpa ada yang hilang atau duplikat")
    void testAllTasksAllocatedWithoutDuplicates() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            tasks.add(new Task(i, 80000 + (i * 1000)));
        }

        Map<WorkerNode, List<Task>> allocation = scheduler.schedule(tasks, workers);

        assertNotNull(allocation);
        assertEquals(3, allocation.size());

        Set<Integer> allocatedTaskIds = new HashSet<>();
        int totalAllocated = 0;
        for (List<Task> assigned : allocation.values()) {
            totalAllocated += assigned.size();
            for (Task t : assigned) {
                assertTrue(allocatedTaskIds.add(t.getId()), "Task ID " + t.getId() + " terduplikasi!");
            }
        }

        assertEquals(tasks.size(), totalAllocated, "Total task yang dialokasikan harus sama dengan total input task");
    }

    @Test
    @DisplayName("Worker dengan MIPS lebih tinggi harus mendapatkan akumulasi workload lebih besar pada DRALBA")
    void testHeterogeneousWorkloadDistribution() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            tasks.add(new Task(i, 90000 + (i * 500)));
        }

        Map<WorkerNode, List<Task>> allocation = scheduler.schedule(tasks, workers);

        long loadW1 = allocation.get(workers.get(0)).stream().mapToLong(Task::getLength).sum();
        long loadW2 = allocation.get(workers.get(1)).stream().mapToLong(Task::getLength).sum();
        long loadW3 = allocation.get(workers.get(2)).stream().mapToLong(Task::getLength).sum();

        assertTrue(loadW3 > loadW2, "Worker-3 (3000 MIPS) harus menanggung load lebih besar dari Worker-2 (2000 MIPS)");
        assertTrue(loadW2 > loadW1, "Worker-2 (2000 MIPS) harus menanggung load lebih besar dari Worker-1 (1000 MIPS)");
    }

    @Test
    @DisplayName("Penjadwalan dengan daftar task kosong harus menghasilkan mapping kosong")
    void testEmptyTasks() {
        Map<WorkerNode, List<Task>> allocation = scheduler.schedule(Collections.emptyList(), workers);
        assertNotNull(allocation);
        for (List<Task> list : allocation.values()) {
            assertTrue(list.isEmpty());
        }
    }

    @Test
    @DisplayName("Exception harus dilempar jika daftar worker kosong")
    void testEmptyWorkersThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                scheduler.schedule(List.of(new Task(1, 1000)), Collections.emptyList()));
    }
}

