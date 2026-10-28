#!/usr/bin/env python3
"""
Script Visualisasi Hasil Benchmark Real-World Cloud Task Scheduling (Algoritma DRALBA).
Membaca file results/benchmark_results.csv dan menghasilkan diagram evaluasi performa DRALBA.
"""

import os
import sys
import pandas as pd
import matplotlib.pyplot as plt

def main():
    csv_file = os.path.join("results", "benchmark_results.csv")
    if not os.path.exists(csv_file):
        print(f"[ERROR] File {csv_file} tidak ditemukan!")
        sys.exit(1)

    df = pd.read_csv(csv_file)
    # Filter hanya algoritma DRALBA
    df = df[df["Algorithm"] == "DRALBA"].copy()
    print(f"[INFO] Membaca {len(df)} baris data DRALBA dari {csv_file}")

    # Ekstrak ukuran dataset angka dari nama "GoCJ_100" -> 100
    df["DatasetSize"] = df["Dataset"].str.replace("GoCJ_", "").astype(int)
    df = df.sort_values(by="DatasetSize")

    # Setup style matplotlib
    plt.style.use("seaborn-v0_8-whitegrid" if "seaborn-v0_8-whitegrid" in plt.style.available else "default")
    fig, axes = plt.subplots(2, 2, figsize=(15, 11))
    fig.suptitle("Evaluasi Performa Real-World Cloud Task Scheduling — Algoritma DRALBA\n(Infrastruktur Docker: 1 Datacenter, 2 Host, 8 Worker VM Heterogen)",
                 fontsize=15, fontweight="bold", y=0.98)

    color = "#1f77b4"  # Biru DRALBA

    # 1. Makespan (detik)
    ax1 = axes[0, 0]
    ax1.plot(df["DatasetSize"], df["MakespanSec"], label="DRALBA",
             color=color, marker="o", linewidth=2.5, markersize=8)
    for x, y in zip(df["DatasetSize"], df["MakespanSec"]):
        ax1.annotate(f"{y:.2f}s", (x, y), textcoords="offset points", xytext=(0, 8), ha="center", fontsize=8.5)
    ax1.set_title("(A) Makespan (Detik) - [Lebih Rendah Lebih Baik]", fontsize=12, fontweight="bold")
    ax1.set_xlabel("Ukuran Dataset (Jumlah Task)", fontsize=10)
    ax1.set_ylabel("Makespan (Detik)", fontsize=10)
    ax1.legend(loc="upper left", frameon=True)
    ax1.grid(True, linestyle="--", alpha=0.6)
    ax1.margins(y=0.15)

    # 2. Throughput (task/detik)
    ax2 = axes[0, 1]
    ax2.plot(df["DatasetSize"], df["ThroughputTasksPerSec"], label="DRALBA",
             color="#2ca02c", marker="s", linewidth=2.5, markersize=8)
    for x, y in zip(df["DatasetSize"], df["ThroughputTasksPerSec"]):
        ax2.annotate(f"{y:.1f}", (x, y), textcoords="offset points", xytext=(0, 8), ha="center", fontsize=8.5)
    ax2.set_title("(B) Throughput (Task / Detik) - [Lebih Tinggi Lebih Baik]", fontsize=12, fontweight="bold")
    ax2.set_xlabel("Ukuran Dataset (Jumlah Task)", fontsize=10)
    ax2.set_ylabel("Throughput (Task / Detik)", fontsize=10)
    ax2.legend(loc="lower right", frameon=True)
    ax2.grid(True, linestyle="--", alpha=0.6)
    ax2.margins(y=0.2)

    # 3. Average Response Time (ms)
    ax3 = axes[1, 0]
    ax3.plot(df["DatasetSize"], df["AverageResponseTimeMs"], label="DRALBA",
             color="#ff7f0e", marker="^", linewidth=2.5, markersize=8)
    for x, y in zip(df["DatasetSize"], df["AverageResponseTimeMs"]):
        ax3.annotate(f"{y:.2f}ms", (x, y), textcoords="offset points", xytext=(0, 8), ha="center", fontsize=8.5)
    ax3.set_title("(C) Average Response Time (ART) (ms) - [Lebih Rendah Lebih Baik]", fontsize=12, fontweight="bold")
    ax3.set_xlabel("Ukuran Dataset (Jumlah Task)", fontsize=10)
    ax3.set_ylabel("ART (Milidetik)", fontsize=10)
    ax3.legend(loc="upper right", frameon=True)
    ax3.grid(True, linestyle="--", alpha=0.6)
    ax3.margins(y=0.2)

    # 4. Average Resource Utilization (ARUR %)
    ax4 = axes[1, 1]
    ax4.plot(df["DatasetSize"], df["ARURPercent"], label="DRALBA",
             color="#9467bd", marker="D", linewidth=2.5, markersize=8)
    for x, y in zip(df["DatasetSize"], df["ARURPercent"]):
        ax4.annotate(f"{y:.2f}%", (x, y), textcoords="offset points", xytext=(0, 8), ha="center", fontsize=8.5)
    ax4.set_title("(D) Average Resource Utilization (ARUR) (%) - [Optimal & Seimbang]", fontsize=12, fontweight="bold")
    ax4.set_xlabel("Ukuran Dataset (Jumlah Task)", fontsize=10)
    ax4.set_ylabel("ARUR (%)", fontsize=10)
    ax4.legend(loc="lower right", frameon=True)
    ax4.grid(True, linestyle="--", alpha=0.6)
    ax4.margins(y=0.2)

    plt.tight_layout(rect=[0, 0.03, 1, 0.95])

    out_img = os.path.join("results", "comparison_metrics.png")
    plt.savefig(out_img, dpi=300)
    print(f"[INFO] Diagram visualisasi DRALBA berhasil disimpan di: {out_img}")

if __name__ == "__main__":
    main()
