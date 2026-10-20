#!/usr/bin/env python3
"""
Script Visualisasi Hasil Benchmark Real-World Cloud Task Scheduling (DRALBA vs FCFS vs Round Robin).
Membaca file results/benchmark_results.csv dan menghasilkan diagram perbandingan performa.
"""

import os
import sys
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker

def main():
    csv_file = os.path.join("results", "benchmark_results.csv")
    if not os.path.exists(csv_file):
        print(f"[ERROR] File {csv_file} tidak ditemukan!")
        sys.exit(1)

    df = pd.read_csv(csv_file)
    print(f"[INFO] Membaca {len(df)} baris data dari {csv_file}")

    # Ekstrak ukuran dataset angka dari nama "GoCJ_100" -> 100
    df["DatasetSize"] = df["Dataset"].str.replace("GoCJ_", "").astype(int)
    df = df.sort_values(by=["DatasetSize", "Algorithm"])

    # Setup style matplotlib
    plt.style.use("seaborn-v0_8-whitegrid" if "seaborn-v0_8-whitegrid" in plt.style.available else "default")
    fig, axes = plt.subplots(2, 2, figsize=(15, 11))
    fig.suptitle("Evaluasi Komparatif Real-World Cloud Task Scheduling\nDRALBA vs FCFS vs Round Robin (Docker Heterogen)",
                 fontsize=15, fontweight="bold", y=0.98)

    palette = {
        "DRALBA": "#1f77b4",       # Biru
        "FCFS": "#ff7f0e",         # Oranye
        "RoundRobin": "#2ca02c"    # Hijau
    }
    markers = {"DRALBA": "o", "FCFS": "s", "RoundRobin": "^"}

    algorithms = df["Algorithm"].unique()

    # 1. Makespan (detik) - Semakin rendah semakin baik
    ax1 = axes[0, 0]
    for algo in algorithms:
        sub = df[df["Algorithm"] == algo]
        ax1.plot(sub["DatasetSize"], sub["MakespanSec"], label=algo,
                 color=palette.get(algo, None), marker=markers.get(algo, "o"), linewidth=2.2, markersize=7)
    ax1.set_title("(A) Makespan (Detik) - [Lebih Rendah Lebih Baik]", fontsize=12, fontweight="bold")
    ax1.set_xlabel("Ukuran Dataset (Jumlah Task)", fontsize=10)
    ax1.set_ylabel("Makespan (Detik)", fontsize=10)
    ax1.legend(loc="upper left", frameon=True)
    ax1.grid(True, linestyle="--", alpha=0.6)

    # 2. Throughput (task/detik) - Semakin tinggi semakin baik
    ax2 = axes[0, 1]
    for algo in algorithms:
        sub = df[df["Algorithm"] == algo]
        ax2.plot(sub["DatasetSize"], sub["ThroughputTasksPerSec"], label=algo,
                 color=palette.get(algo, None), marker=markers.get(algo, "o"), linewidth=2.2, markersize=7)
    ax2.set_title("(B) Throughput (Task / Detik) - [Lebih Tinggi Lebih Baik]", fontsize=12, fontweight="bold")
    ax2.set_xlabel("Ukuran Dataset (Jumlah Task)", fontsize=10)
    ax2.set_ylabel("Throughput (Task / Detik)", fontsize=10)
    ax2.legend(loc="lower right", frameon=True)
    ax2.grid(True, linestyle="--", alpha=0.6)

    # 3. Average Response Time (ms) - Semakin rendah semakin baik
    ax3 = axes[1, 0]
    for algo in algorithms:
        sub = df[df["Algorithm"] == algo]
        ax3.plot(sub["DatasetSize"], sub["AverageResponseTimeMs"], label=algo,
                 color=palette.get(algo, None), marker=markers.get(algo, "o"), linewidth=2.2, markersize=7)
    ax3.set_title("(C) Average Response Time (ART) (ms) - [Lebih Rendah Lebih Baik]", fontsize=12, fontweight="bold")
    ax3.set_xlabel("Ukuran Dataset (Jumlah Task)", fontsize=10)
    ax3.set_ylabel("ART (Milidetik)", fontsize=10)
    ax3.legend(loc="upper left", frameon=True)
    ax3.grid(True, linestyle="--", alpha=0.6)

    # 4. Average Resource Utilization (ARUR %) - Semakin tinggi semakin optimal
    ax4 = axes[1, 1]
    for algo in algorithms:
        sub = df[df["Algorithm"] == algo]
        ax4.plot(sub["DatasetSize"], sub["ARURPercent"], label=algo,
                 color=palette.get(algo, None), marker=markers.get(algo, "o"), linewidth=2.2, markersize=7)
    ax4.set_title("(D) Average Resource Utilization (ARUR) (%) - [Optimal & Seimbang]", fontsize=12, fontweight="bold")
    ax4.set_xlabel("Ukuran Dataset (Jumlah Task)", fontsize=10)
    ax4.set_ylabel("ARUR (%)", fontsize=10)
    ax4.legend(loc="upper right", frameon=True)
    ax4.grid(True, linestyle="--", alpha=0.6)

    plt.tight_layout(rect=[0, 0.03, 1, 0.95])

    out_img = os.path.join("results", "comparison_metrics.png")
    plt.savefig(out_img, dpi=300)
    print(f"[INFO] Diagram visualisasi berhasil disimpan di: {out_img}")

if __name__ == "__main__":
    main()

