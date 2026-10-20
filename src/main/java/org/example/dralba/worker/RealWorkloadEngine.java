package org.example.dralba.worker;

import org.example.dralba.model.Task;
import org.example.dralba.model.TaskExecutionResult;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Engine komputasi nyata (CPU-bound workload).
 * Menjalankan operasi aritmatika, hashing kriptografis, dan transformasi bitwise
 * dengan jumlah siklus yang proporsional terhadap task length dari dataset GoCJ.
 * BUKAN sekadar simulasi atau Thread.sleep.
 */
public class RealWorkloadEngine {

    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final int scaleFactor;

    public RealWorkloadEngine() {
        this(resolveScaleFactor());
    }

    public RealWorkloadEngine(int scaleFactor) {
        this.scaleFactor = Math.max(1, scaleFactor);
    }

    private static int resolveScaleFactor() {
        String env = System.getenv("WORKLOAD_SCALE_FACTOR");
        if (env != null && !env.isBlank()) {
            try {
                return Integer.parseInt(env.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 10; // Default: 10 iterasi per unit length dataset GoCJ
    }

    /**
     * Menjalankan beban komputasi nyata untuk sebuah task.
     *
     * @param task     task dengan panjang instruksi dari GoCJ
     * @param workerId ID worker yang mengeksekusi
     * @return TaskExecutionResult yang berisi durasi wall-clock, CPU time, dan checksum hasil
     */
    public TaskExecutionResult execute(Task task, String workerId) {
        long submitTime = System.currentTimeMillis();
        long startCpuNanos = getCpuTimeNanos();
        long startWallNanos = System.nanoTime();
        long startTime = System.currentTimeMillis();

        String status = "SUCCESS";
        String errorMessage = null;
        String checksum = "";

        try {
            checksum = computeCpuIntensiveWorkload(task.getId(), task.getLength(), scaleFactor);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
        }

        long finishWallNanos = System.nanoTime();
        long finishCpuNanos = getCpuTimeNanos();
        long finishTime = System.currentTimeMillis();

        double wallDurationMs = (finishWallNanos - startWallNanos) / 1_000_000.0;
        double cpuDurationMs = (finishCpuNanos >= startCpuNanos)
                ? (finishCpuNanos - startCpuNanos) / 1_000_000.0
                : wallDurationMs;

        return new TaskExecutionResult(
                task.getId(),
                workerId,
                task.getLength(),
                status,
                submitTime,
                startTime,
                finishTime,
                wallDurationMs,
                cpuDurationMs,
                checksum,
                errorMessage
        );
    }

    /**
     * Komputasi CPU-bound nyata: kombinasi PRNG matematis (Xorshift64*),
     * modular exponentiation, dan SHA-256 digest hashing.
     */
    private String computeCpuIntensiveWorkload(int taskId, long length, int factor) throws NoSuchAlgorithmException {
        long totalIterations = length * factor;
        long state = (long) taskId ^ (length * 0x9E3779B97F4A7C15L);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[32];

        // Jalankan loop aritmatika dan mutasi buffer
        for (long i = 0; i < totalIterations; i++) {
            // Xorshift64* pseudo-random step
            state ^= (state >> 12);
            state ^= (state << 25);
            state ^= (state >> 27);
            long mult = state * 0x2545F4914F6CDD1DL;

            int idx = (int) (i & 31);
            buffer[idx] = (byte) (buffer[idx] ^ (mult & 0xFF));

            // Setiap 10.000 iterasi, lakukan update digest untuk mencegah compiler optimization
            if ((i & 0x1FFF) == 0) {
                digest.update(buffer);
            }
        }

        digest.update(buffer);
        byte[] hash = digest.digest();

        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private long getCpuTimeNanos() {
        if (threadBean.isCurrentThreadCpuTimeSupported()) {
            return threadBean.getCurrentThreadCpuTime();
        }
        return System.nanoTime();
    }

    public int getScaleFactor() {
        return scaleFactor;
    }
}

