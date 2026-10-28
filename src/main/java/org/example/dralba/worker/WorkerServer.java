package org.example.dralba.worker;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.example.dralba.model.Task;
import org.example.dralba.model.TaskExecutionResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Worker HTTP Server yang berjalan di dalam container Docker.
 * Menerima request eksekusi task, menjalankan RealWorkloadEngine,
 * dan mengembalikan metrik eksekusi aktual.
 */
public class WorkerServer {

    private final String workerId;
    private final String hostId;
    private final String datacenterId;
    private final int port;
    private final double capacityMips;
    private final int cores;
    private final RealWorkloadEngine workloadEngine;
    private final Gson gson = new Gson();

    private final AtomicInteger tasksExecuted = new AtomicInteger(0);
    private final DoubleAdder totalBusyTimeMs = new DoubleAdder();
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    private HttpServer server;

    public WorkerServer(String workerId, int port, double capacityMips, int cores, int scaleFactor) {
        this(workerId, port, capacityMips, cores, scaleFactor, "host-1", "datacenter-1");
    }

    public WorkerServer(String workerId, int port, double capacityMips, int cores, int scaleFactor, String hostId, String datacenterId) {
        this.workerId = workerId;
        this.hostId = (hostId != null && !hostId.isBlank()) ? hostId : "host-1";
        this.datacenterId = (datacenterId != null && !datacenterId.isBlank()) ? datacenterId : "datacenter-1";
        this.port = port;
        this.capacityMips = capacityMips;
        this.cores = cores;
        this.workloadEngine = new RealWorkloadEngine(scaleFactor);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.max(1, cores));
        server.setExecutor(executor);

        server.createContext("/health", new HealthHandler());
        server.createContext("/execute", new ExecuteHandler());
        server.createContext("/metrics", new MetricsHandler());
        server.createContext("/reset", new ResetHandler());

        server.start();
        System.out.printf("[%s | %s | %s] Worker aktif di port %d (MIPS: %.0f, Cores: %d, Scale: %d)%n",
                datacenterId, hostId, workerId, port, capacityMips, cores, workloadEngine.getScaleFactor());
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            System.out.printf("[%s] Worker dihentikan.%n", workerId);
        }
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "UP");
            resp.put("workerId", workerId);
            resp.put("hostId", hostId);
            resp.put("datacenterId", datacenterId);
            resp.put("capacityMips", capacityMips);
            resp.put("cores", cores);
            resp.put("scaleFactor", workloadEngine.getScaleFactor());
            sendJsonResponse(exchange, 200, resp);
        }
    }

    private class ExecuteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Task task = gson.fromJson(body, Task.class);

            if (task == null) {
                sendResponse(exchange, 400, "Bad Request: JSON body task kosong");
                return;
            }

            activeTasks.incrementAndGet();
            try {
                TaskExecutionResult result = workloadEngine.execute(task, workerId);
                tasksExecuted.incrementAndGet();
                totalBusyTimeMs.add(result.getExecutionTimeMs());
                sendJsonResponse(exchange, 200, result);
            } finally {
                activeTasks.decrementAndGet();
            }
        }
    }

    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> resp = new HashMap<>();
            resp.put("workerId", workerId);
            resp.put("tasksExecuted", tasksExecuted.get());
            resp.put("totalBusyTimeMs", totalBusyTimeMs.sum());
            resp.put("activeTasks", activeTasks.get());
            sendJsonResponse(exchange, 200, resp);
        }
    }

    private class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            tasksExecuted.set(0);
            totalBusyTimeMs.reset();
            activeTasks.set(0);
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "RESET_OK");
            resp.put("workerId", workerId);
            sendJsonResponse(exchange, 200, resp);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void main(String[] args) throws Exception {
        String workerId = getEnvOrDefault("WORKER_ID", "vm-1");
        String hostId = getEnvOrDefault("HOST_ID", "host-1");
        String datacenterId = getEnvOrDefault("DATACENTER_ID", "datacenter-1");
        int port = Integer.parseInt(getEnvOrDefault("PORT", "8080"));
        double capacityMips = Double.parseDouble(getEnvOrDefault("CAPACITY_MIPS", "1000.0"));
        int cores = Integer.parseInt(getEnvOrDefault("CORES", "1"));
        int scaleFactor = Integer.parseInt(getEnvOrDefault("WORKLOAD_SCALE_FACTOR", "10"));

        WorkerServer server = new WorkerServer(workerId, port, capacityMips, cores, scaleFactor, hostId, datacenterId);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        // Jaga agar process tetap berjalan
        Thread.currentThread().join();
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val.trim() : defaultValue;
    }
}

