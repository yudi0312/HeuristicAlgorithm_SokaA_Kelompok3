package org.example.dralba.scheduler;

import com.google.gson.Gson;
import org.example.dralba.model.Task;
import org.example.dralba.model.TaskExecutionResult;
import org.example.dralba.model.WorkerNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Dispatcher yang mengirimkan task terjadwal ke setiap container worker secara paralel
 * melalui protokol HTTP REST API.
 */
public class RealWorldDispatcher {

    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public RealWorldDispatcher() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Memeriksa kesiapan (health check) seluruh worker container.
     */
    public boolean checkWorkersHealth(List<WorkerNode> workers) {
        for (WorkerNode worker : workers) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(worker.getEndpointUrl() + "/health"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    System.err.printf("[WARN] Worker %s tidak sehat (status %d)%n", worker.getId(), response.statusCode());
                    return false;
                }
            } catch (Exception e) {
                System.err.printf("[ERROR] Gagal menghubungi worker %s di %s: %s%n",
                        worker.getId(), worker.getEndpointUrl(), e.getMessage());
                return false;
            }
        }
        return true;
    }

    /**
     * Me-reset status metrik internal di seluruh worker container sebelum eksperimen baru.
     */
    public void resetWorkers(List<WorkerNode> workers) {
        for (WorkerNode worker : workers) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(worker.getEndpointUrl() + "/reset"))
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                System.err.printf("[WARN] Reset worker %s gagal: %s%n", worker.getId(), e.getMessage());
            }
        }
    }

    /**
     * Mendispatch seluruh task sesuai mapping alokasi ke worker containers secara paralel.
     * Setiap worker mengeksekusi antrean task miliknya sendiri.
     */
    public List<TaskExecutionResult> dispatch(Map<WorkerNode, List<Task>> scheduledAllocation) {
        List<TaskExecutionResult> allResults = new CopyOnWriteArrayList<>();
        ExecutorService workerPool = Executors.newFixedThreadPool(Math.max(1, scheduledAllocation.size()));
        List<Future<?>> workerFutures = new ArrayList<>();

        for (Map.Entry<WorkerNode, List<Task>> entry : scheduledAllocation.entrySet()) {
            WorkerNode worker = entry.getKey();
            List<Task> assignedTasks = entry.getValue();

            workerFutures.add(workerPool.submit(() -> {
                // Eksekusi setiap task yang dialokasikan ke worker ini
                for (Task task : assignedTasks) {
                    long submitTime = System.currentTimeMillis();
                    TaskExecutionResult result = sendTaskToWorker(worker, task, submitTime);
                    allResults.add(result);
                }
            }));
        }

        // Tunggu seluruh worker selesai
        for (Future<?> f : workerFutures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("[ERROR] Eksekusi worker terinterupsi: " + e.getMessage());
            }
        }
        workerPool.shutdown();

        // Urutkan kembali berdasarkan taskId
        allResults.sort(Comparator.comparingInt(TaskExecutionResult::getTaskId));
        return allResults;
    }

    private TaskExecutionResult sendTaskToWorker(WorkerNode worker, Task task, long submitTime) {
        String url = worker.getEndpointUrl() + "/execute";
        String jsonPayload = gson.toJson(task);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                TaskExecutionResult res = gson.fromJson(response.body(), TaskExecutionResult.class);
                return (res != null) ? res : new TaskExecutionResult(
                        task.getId(), worker.getId(), task.getLength(), "FAILED",
                        submitTime, submitTime, System.currentTimeMillis(),
                        0, 0, "", "Hasil kosong dari worker"
                );
            } else {
                return new TaskExecutionResult(
                        task.getId(), worker.getId(), task.getLength(), "FAILED",
                        submitTime, submitTime, System.currentTimeMillis(),
                        0, 0, "", "HTTP " + response.statusCode() + ": " + response.body()
                );
            }
        } catch (Exception e) {
            return new TaskExecutionResult(
                    task.getId(), worker.getId(), task.getLength(), "FAILED",
                    submitTime, submitTime, System.currentTimeMillis(),
                    0, 0, "", "Koneksi gagal: " + e.getMessage()
            );
        }
    }
}

