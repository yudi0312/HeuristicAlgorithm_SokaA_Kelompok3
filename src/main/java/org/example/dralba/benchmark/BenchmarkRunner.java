package org.example.dralba.benchmark;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.dralba.algorithm.DRALBAScheduler;
import org.example.dralba.algorithm.FCFSScheduler;
import org.example.dralba.algorithm.RoundRobinScheduler;
import org.example.dralba.algorithm.TaskScheduler;
import org.example.dralba.model.EvaluationMetrics;
import org.example.dralba.model.Task;
import org.example.dralba.model.TaskExecutionResult;
import org.example.dralba.model.WorkerNode;
import org.example.dralba.scheduler.MetricsCalculator;
import org.example.dralba.scheduler.RealWorldDispatcher;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Entry point utama eksperimen benchmark real-world.
 * Menjalankan evaluasi komparatif antara DRALBA, FCFS, dan Round Robin
 * pada infrastruktur Docker worker heterogen menggunakan dataset GoCJ.
 */
public class BenchmarkRunner {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println("     REAL-WORLD CLOUD TASK SCHEDULING BENCHMARK (DRALBA & DOCKER)       ");
        System.out.println("========================================================================");

        int datasetSize = 100;
        String outputDir = "results";
        boolean runComparison = true;
        List<Integer> datasetSizesToRun = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if ("--dataset".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                datasetSize = Integer.parseInt(args[++i]);
            } else if ("--output-dir".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                outputDir = args[++i];
            } else if ("--multi".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                String[] parts = args[++i].split(",");
                for (String p : parts) {
                    datasetSizesToRun.add(Integer.parseInt(p.trim()));
                }
            }
        }

        if (datasetSizesToRun.isEmpty()) {
            datasetSizesToRun.add(datasetSize);
        }

        List<WorkerNode> workers = resolveWorkers();
        System.out.printf("Infrastruktur Worker Terdeteksi (%d worker):%n", workers.size());
        for (WorkerNode w : workers) {
            System.out.printf("  - [%s] %s | URL: %s | MIPS: %.0f | Cores: %d%n",
                    w.getId(), w.getName(), w.getEndpointUrl(), w.getCapacityMips(), w.getCores());
        }

        RealWorldDispatcher dispatcher = new RealWorldDispatcher();

        System.out.println("\nMemeriksa kesiapan (health check) worker containers...");
        int retries = 5;
        boolean healthy = false;
        while (retries-- > 0) {
            if (dispatcher.checkWorkersHealth(workers)) {
                healthy = true;
                break;
            }
            System.out.println("  Menunggu worker siap (retrying in 2s)...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
        }

        if (!healthy) {
            System.err.println("[FATAL] Sebagian atau seluruh worker tidak dapat dihubungi. Pastikan Docker Compose telah berjalan.");
            System.exit(1);
        }
        System.out.println("Seluruh worker sehat dan siap menerima workload!\n");

        List<TaskScheduler> schedulers = List.of(
                new DRALBAScheduler(),
                new FCFSScheduler(),
                new RoundRobinScheduler()
        );

        List<EvaluationMetrics> allMetrics = new ArrayList<>();

        for (int size : datasetSizesToRun) {
            System.out.printf("%n========================================================================%n");
            System.out.printf(">>> MENJALANKAN EKSPERIMEN UNTUK DATASET: GoCJ_%d.txt <<<%n", size);
            System.out.printf("========================================================================%n");

            List<Task> tasks = loadDataset(size);
            if (tasks.isEmpty()) {
                System.err.printf("[WARN] Dataset ukuran %d kosong atau tidak ditemukan. Dilewati.%n", size);
                continue;
            }
            System.out.printf("Memuat %d task dari GoCJ_Dataset_%d.txt%n", tasks.size(), size);

            for (TaskScheduler scheduler : schedulers) {
                System.out.printf("%n--- Menjalankan Scheduler: %s ---%n", scheduler.getName());

                // 1. Ukur scheduling overhead
                long schedStart = System.nanoTime();
                Map<WorkerNode, List<Task>> allocation = scheduler.schedule(tasks, workers);
                long schedEnd = System.nanoTime();
                double schedulingOverheadMs = (schedEnd - schedStart) / 1_000_000.0;

                // Tampilkan distribusi task
                System.out.print("Distribusi task: ");
                for (Map.Entry<WorkerNode, List<Task>> e : allocation.entrySet()) {
                    System.out.printf("[%s: %d tasks] ", e.getKey().getId(), e.getValue().size());
                }
                System.out.println();

                // 2. Reset worker stats
                dispatcher.resetWorkers(workers);

                // 3. Dispatch tasks & ukur real wall-clock execution
                long globalStart = System.currentTimeMillis();
                List<TaskExecutionResult> results = dispatcher.dispatch(allocation);
                long globalFinish = System.currentTimeMillis();

                // 4. Hitung metrik evaluasi
                EvaluationMetrics metrics = MetricsCalculator.calculate(
                        scheduler.getName(),
                        "GoCJ_" + size,
                        results,
                        workers,
                        schedulingOverheadMs,
                        globalStart,
                        globalFinish
                );

                allMetrics.add(metrics);

                System.out.printf("Selesai: Makespan = %.2f detik | Throughput = %.2f task/s | ART = %.2f ms | ARUR = %.2f%% | LB StdDev = %.2f ms%n",
                        metrics.getMakespanSeconds(),
                        metrics.getThroughputTasksPerSec(),
                        metrics.getAverageResponseTimeMs(),
                        metrics.getAverageResourceUtilizationPercent(),
                        metrics.getLoadBalancingStdDevMs());
            }
        }

        // Tampilkan tabel perbandingan akhir
        printComparisonTable(allMetrics);

        // Ekspor ke file CSV dan JSON
        exportResults(allMetrics, outputDir);
    }

    public static List<Task> loadDataset(int size) {
        List<Task> tasks = new ArrayList<>();
        String filename = "GoCJ_Dataset_" + size + ".txt";

        // Coba dari file system langsung
        Path directPath = Paths.get("src", "main", "resources", filename);
        if (!Files.exists(directPath)) {
            directPath = Paths.get("resources", filename);
        }
        if (!Files.exists(directPath)) {
            directPath = Paths.get(filename);
        }

        InputStream is = null;
        try {
            if (Files.exists(directPath)) {
                is = Files.newInputStream(directPath);
            } else {
                is = BenchmarkRunner.class.getResourceAsStream("/" + filename);
            }

            if (is == null) {
                System.err.println("[ERROR] File dataset tidak ditemukan: " + filename);
                return tasks;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                int id = 1;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        try {
                            long len = Long.parseLong(line);
                            tasks.add(new Task(id++, len));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Gagal membaca dataset: " + e.getMessage());
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException ignored) {}
            }
        }
        return tasks;
    }

    public static List<WorkerNode> resolveWorkers() {
        String urlsEnv = System.getenv("WORKER_URLS");
        String capsEnv = System.getenv("WORKER_CAPACITIES");

        List<WorkerNode> workers = new ArrayList<>();

        if (urlsEnv != null && !urlsEnv.isBlank()) {
            String[] urls = urlsEnv.split(",");
            String[] caps = (capsEnv != null && !capsEnv.isBlank()) ? capsEnv.split(",") : new String[0];

            for (int i = 0; i < urls.length; i++) {
                String url = urls[i].trim();
                String id = "worker-" + (i + 1);
                double mips = (i < caps.length) ? Double.parseDouble(caps[i].trim()) : (1000.0 * (i + 1));
                workers.add(new WorkerNode(id, "Worker " + (i + 1), url, mips, 1));
            }
        } else {
            // Default 3 heterogeneous workers for local or standard compose
            workers.add(new WorkerNode("worker-1", "Worker-1 (Low)", "http://localhost:8081", 1000.0, 1));
            workers.add(new WorkerNode("worker-2", "Worker-2 (Medium)", "http://localhost:8082", 2000.0, 1));
            workers.add(new WorkerNode("worker-3", "Worker-3 (High)", "http://localhost:8083", 3000.0, 2));
        }

        return workers;
    }

    private static void printComparisonTable(List<EvaluationMetrics> metricsList) {
        System.out.println("\n=========================================================================================================");
        System.out.println("                                RINGKASAN HASIL EVALUASI KOMPARATIF                                      ");
        System.out.println("=========================================================================================================");
        System.out.printf("%-12s | %-12s | %-12s | %-14s | %-12s | %-10s | %-14s%n",
                "Dataset", "Algoritma", "Makespan(s)", "Throughput(t/s)", "ART (ms)", "ARUR (%)", "LB StdDev (ms)");
        System.out.println("---------------------------------------------------------------------------------------------------------");

        for (EvaluationMetrics m : metricsList) {
            System.out.printf("%-12s | %-12s | %12.2f | %14.2f | %12.2f | %9.2f%% | %14.2f%n",
                    m.getDatasetName(),
                    m.getAlgorithmName(),
                    m.getMakespanSeconds(),
                    m.getThroughputTasksPerSec(),
                    m.getAverageResponseTimeMs(),
                    m.getAverageResourceUtilizationPercent(),
                    m.getLoadBalancingStdDevMs());
        }
        System.out.println("=========================================================================================================\n");
    }

    private static void exportResults(List<EvaluationMetrics> metricsList, String outputDir) {
        try {
            Path outDir = Paths.get(outputDir);
            Files.createDirectories(outDir);

            // Export to CSV
            Path csvPath = outDir.resolve("benchmark_results.csv");
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8))) {
                pw.println("Dataset,Algorithm,TotalTasks,SuccessfulTasks,FailedTasks,MakespanSec,ThroughputTasksPerSec,AverageResponseTimeMs,AverageWaitingTimeMs,ARURPercent,LoadBalancingStdDevMs,SchedulingOverheadMs");
                for (EvaluationMetrics m : metricsList) {
                    pw.printf("%s,%s,%d,%d,%d,%.4f,%.4f,%.2f,%.2f,%.2f,%.2f,%.4f%n",
                            m.getDatasetName(),
                            m.getAlgorithmName(),
                            m.getTotalTasks(),
                            m.getSuccessfulTasks(),
                            m.getFailedTasks(),
                            m.getMakespanSeconds(),
                            m.getThroughputTasksPerSec(),
                            m.getAverageResponseTimeMs(),
                            m.getAverageWaitingTimeMs(),
                            m.getAverageResourceUtilizationPercent(),
                            m.getLoadBalancingStdDevMs(),
                            m.getSchedulingOverheadMs());
                }
            }
            System.out.println("[INFO] Hasil berhasil diekspor ke CSV: " + csvPath.toAbsolutePath());

            // Export to JSON
            Path jsonPath = outDir.resolve("benchmark_results.json");
            try (Writer writer = Files.newBufferedWriter(jsonPath, StandardCharsets.UTF_8)) {
                gson.toJson(metricsList, writer);
            }
            System.out.println("[INFO] Hasil berhasil diekspor ke JSON: " + jsonPath.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("[ERROR] Gagal mengekspor hasil benchmark: " + e.getMessage());
        }
    }
}

