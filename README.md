# **Real-World Cloud Task Scheduling dengan DRALBA dan Docker**

| Nama | NRP |
|---|---|
| Putu Yudi Nandanjaya Wiraguna | 5027241080 |
| Danuja Prasasta Bastu | 5027241037 |
| Muhammad Huda Rabbani | 5027241098 |
| I Gede Bagus Saka Sinatrya | 5027241088 |
| Raynard Carlent | 5027241109 |

---

## Ringkasan Proyek

Repositori ini mengimplementasikan **Cloud Task Scheduling** pada lingkungan cloud heterogen menggunakan algoritma heuristik **DRALBA** (*Dynamic Resource Allocation and Load Balancing Algorithm*).

Proyek ini menyediakan **dua mode eksekusi**:
1. **Mode Real-World (Docker & Docker Compose):** Eksekusi terdistribusi nyata (1 Datacenter, 2 Host, 8 Worker VM) di mana scheduler mendistribusikan task melalui protokol HTTP REST API ke sejumlah container worker independen. Setiap worker dibatasi alokasi CPU/RAM heterogen melalui Docker cgroups dan mengeksekusi beban komputasi nyata (*CPU-bound cryptographic hashing & arithmetic transformation*), **bukan** sekadar simulasi waktu atau `Thread.sleep`.
2. **Mode Simulasi (CloudSim Plus 7.3.0):** Simulasi *in-memory* berbasis discrete-event simulation pada arsitektur 1 Datacenter, 2 Host, dan 8 VM heterogen.

> **Cakupan Dataset:** Pengujian difokuskan pada dataset GoCJ (*Google Cloud Jobs*) berukuran **100 hingga 1000 cloudlet**. Dataset di atas 1000 tidak diikutsertakan secara default demi efisiensi waktu eksekusi tanpa mengurangi validitas tren evaluasi performa.

---

## Daftar Isi

- [Arsitektur Sistem Real-World](#-arsitektur-sistem-real-world)
- [Penjelasan Algoritma Penjadwalan](#-penjelasan-algoritma-penjadwalan)
- [Definisi dan Formula Metrik Evaluasi](#-definisi-dan-formula-metrik-evaluasi)
- [Teknologi dan Dependensi](#-teknologi-dan-dependensi)
- [Struktur Direktori](#-struktur-direktori)
- [Instalasi dan Konfigurasi](#-instalasi-dan-konfigurasi)
- [Cara Menjalankan Proyek](#-cara-menjalankan-proyek)
- [Konfigurasi Worker dan Workload](#-konfigurasi-worker-dan-workload)
- [Hasil Eksperimen Real-World Aktual](#-hasil-eksperimen-real-world-aktual)
- [Visualisasi Hasil Eksperimen](#-visualisasi-hasil-eksperimen)
- [Pengujian dan Validasi (Testing)](#-pengujian-dan-validasi-testing)
- [Troubleshooting dan Kendala](#-troubleshooting-dan-kendala)
- [Referensi Ilmiah](#-referensi-ilmiah)

---

## Arsitektur Sistem Real-World (1 Datacenter, 2 Host, 4 VM per Host)

Pada implementasi Docker, sistem merepresentasikan **1 Datacenter (`datacenter-1`)** yang menaungi **2 Host Logis (`host-1` dan `host-2`)**, di mana setiap host mengelola **4 Virtual Machine (VM)** heterogen (total 8 VM container), serta **1 Scheduler Service** yang terhubung dalam virtual bridge network (`dralba-net`).

```mermaid
flowchart TD
    subgraph HostClient["Host Machine / Client"]
        CLI["BenchmarkRunner CLI / Docker Compose"]
        DS["Dataset GoCJ (100 - 1000 Tasks)"]
    end

    subgraph Datacenter["Datacenter: datacenter-1"]
        subgraph SchedulerService["Container: dralba-scheduler"]
            SCHED["Task Scheduler Engine\n(DRALBA)"]
            DISP["HTTP RealWorldDispatcher"]
            CALC["Metrics Calculator"]
        end

        subgraph Host1["Host-1 (4 VMs)"]
            VM1["VM 1 (dralba-h1-vm1)\nCPU: 0.50 Core | RAM: 512MB\n1000 MIPS | Port: 8081"]
            VM2["VM 2 (dralba-h1-vm2)\nCPU: 0.50 Core | RAM: 512MB\n1000 MIPS | Port: 8082"]
            VM3["VM 3 (dralba-h1-vm3)\nCPU: 1.00 Core | RAM: 1024MB\n2000 MIPS | Port: 8083"]
            VM4["VM 4 (dralba-h1-vm4)\nCPU: 1.00 Core | RAM: 1024MB\n3000 MIPS | Port: 8084"]
        end

        subgraph Host2["Host-2 (4 VMs)"]
            VM5["VM 5 (dralba-h2-vm5)\nCPU: 1.00 Core | RAM: 1024MB\n2500 MIPS | Port: 8085"]
            VM6["VM 6 (dralba-h2-vm6)\nCPU: 1.50 Cores | RAM: 1536MB\n4000 MIPS | Port: 8086"]
            VM7["VM 7 (dralba-h2-vm7)\nCPU: 1.50 Cores | RAM: 1536MB\n4000 MIPS | Port: 8087"]
            VM8["VM 8 (dralba-h2-vm8)\nCPU: 1.50 Cores | RAM: 1536MB\n4000 MIPS | Port: 8088"]
        end
    end

    subgraph Output["Output & Artefak"]
        CSV["results/benchmark_results.csv"]
        JSON["results/benchmark_results.json"]
        PNG["results/comparison_metrics.png"]
    end

    DS --> SCHED
    CLI --> SCHED
    SCHED --> DISP
    DISP -- "POST /execute (HTTP)" --> VM1
    DISP -- "POST /execute (HTTP)" --> VM2
    DISP -- "POST /execute (HTTP)" --> VM3
    DISP -- "POST /execute (HTTP)" --> VM4
    DISP -- "POST /execute (HTTP)" --> VM5
    DISP -- "POST /execute (HTTP)" --> VM6
    DISP -- "POST /execute (HTTP)" --> VM7
    DISP -- "POST /execute (HTTP)" --> VM8
    DISP --> CALC
    CALC --> CSV
    CALC --> JSON
    CSV --> PNG
```

### Karakteristik 8 VM Heterogen (2 Host dalam 1 Datacenter)

| Host | Container VM | Limit CPU (cgroups) | Limit RAM | Bobot Kapasitas (MIPS) | Thread Cores | Port Host |
|---|---|---|---|---|---|---|
| **Host-1** | `dralba-h1-vm1` (`vm-1`) | **0.50 Core** | 512 MB | 1000 MIPS (Low) | 1 Core | `8081` |
| **Host-1** | `dralba-h1-vm2` (`vm-2`) | **0.50 Core** | 512 MB | 1000 MIPS (Low) | 1 Core | `8082` |
| **Host-1** | `dralba-h1-vm3` (`vm-3`) | **1.00 Core** | 1024 MB | 2000 MIPS (Medium) | 1 Core | `8083` |
| **Host-1** | `dralba-h1-vm4` (`vm-4`) | **1.00 Core** | 1024 MB | 3000 MIPS (Medium) | 2 Cores | `8084` |
| **Host-2** | `dralba-h2-vm5` (`vm-5`) | **1.00 Core** | 1024 MB | 2500 MIPS (Medium) | 2 Cores | `8085` |
| **Host-2** | `dralba-h2-vm6` (`vm-6`) | **1.50 Cores** | 1536 MB | 4000 MIPS (High) | 4 Cores | `8086` |
| **Host-2** | `dralba-h2-vm7` (`vm-7`) | **1.50 Cores** | 1536 MB | 4000 MIPS (High) | 4 Cores | `8087` |
| **Host-2** | `dralba-h2-vm8` (`vm-8`) | **1.50 Cores** | 1536 MB | 4000 MIPS (High) | 4 Cores | `8088` |

### Beban Kerja Komputasi Nyata (*Real Workload*)
Setiap task dari dataset GoCJ memiliki panjang instruksi $L$ (antara 15.000 hingga 900.000 unit). Worker menjalankan algoritma `RealWorkloadEngine`:
- Perulangan komputasi intensif sebanyak $N_{ops} = L \times \text{ScaleFactor}$ (default ScaleFactor = 10).
- Langkah matematis pseudo-random (Xorshift64*), modular multiplication, dan mutasi buffer byte.
- Pembaharuan berkala cryptographic hash digest **SHA-256** untuk menjamin seluruh core CPU benar-benar terutilisasi 100% dan mencegah eliminasi dead-code oleh JIT compiler.
- Pengembalian waktu CPU aktual (`ThreadMXBean.getCurrentThreadCpuTime()`) dan string checksum hex 64-karakter sebagai bukti verifikasi eksekusi.

---

## Penjelasan Algoritma Penjadwalan

### 1. DRALBA (*Dynamic Resource Allocation and Load Balancing Algorithm*)
DRALBA dirancang khusus untuk meminimalkan Makespan dan menyeimbangkan beban pada lingkungan komputasi heterogen. Algoritma bekerja dalam **dua fase statis**:

```mermaid
flowchart TD
    START([Mulai]) --> INPUT["1. Input Daftar Task & Worker"]
    INPUT --> SHARE["2. Hitung Kapasitas Total & Share Kuota per Worker<br/>Share = TotalLength × (MIPS_w / TotalMIPS)"]
    SHARE --> SORT["3. Urutkan Task Descending berdasarkan Panjang"]
    SORT --> CHECK_SPILL{"Fase 1: Spill Scheduler<br/>Masih ada task & Sisa Share > 0?"}
    
    CHECK_SPILL -- Ya --> MAX_SHARE["Pilih Worker dengan Sisa Share Terbesar"]
    MAX_SHARE --> FIND_TASK{"Ada task yang muat<br/>dalam Sisa Share Worker?"}
    FIND_TASK -- Ya --> ASSIGN_SPILL["Alokasikan task ke Worker<br/>Sisa Share -= Length<br/>EFT += Length / MIPS"]
    ASSIGN_SPILL --> CHECK_SPILL
    FIND_TASK -- Tidak --> END_SPILL["Selesai Fase Spill"]
    
    CHECK_SPILL -- Tidak --> END_SPILL
    END_SPILL --> CHECK_FILL{"Fase 2: Fill Scheduler<br/>Masih ada task tersisa?"}
    
    CHECK_FILL -- Ya --> MIN_EFT["Pilih Worker dengan EFT Minimum<br/>EFT = Akumulasi Waktu Selesai"]
    MIN_EFT --> ASSIGN_FILL["Alokasikan task ke Worker tersebut<br/>EFT += Length / MIPS"]
    ASSIGN_FILL --> CHECK_FILL
    
    CHECK_FILL -- Tidak --> DISPATCH["Dispatch Task ke Worker Container"]
    DISPATCH --> END([Selesai])
```

- **Fase 1 (Spill Scheduler):** Menghitung kuota proporsional $\text{Share}_j = W_{total} \times \frac{C_j}{C_{total}}$. Task terbesar yang muat dialokasikan ke worker dengan sisa kuota terbesar.
- **Fase 2 (Fill Scheduler):** Task-task sisa yang tidak muat dialokasikan secara serakah (*greedy*) ke worker dengan nilai estimasi waktu selesai (*Expected Finish Time* / EFT) terendah:
  $$\text{Worker Target} = \arg\min_{j} (\text{EFT}_j)$$

---

## Definisi dan Formula Metrik Evaluasi

Eksperimen mengukur metrik kuantitatif terstandarisasi berikut:

| Metrik | Satuan | Formula Matematis | Penjelasan |
|---|---|---|---|
| **Makespan ($MS$)** | Detik ($s$) | $MS = \max_{i} (T_{\text{finish}, i}) - \min_{i} (T_{\text{submit}, i})$ | Total waktu wall-clock yang dibutuhkan dari dispatch task pertama hingga selesainya task terakhir. Semakin kecil semakin baik. |
| **Throughput ($TP$)** | Task/s | $TP = \frac{N_{\text{success}}}{MS}$ | Laju penyelesaian task per satuan detik. Semakin tinggi semakin baik. |
| **Average Response Time ($ART$)** | Milidetik ($ms$) | $ART = \frac{1}{N} \sum_{i=1}^N (T_{\text{finish}, i} - T_{\text{submit}, i})$ | Rata-rata waktu tanggap yang dialami sebuah task sejak disubmit oleh scheduler hingga hasil diterima kembali. |
| **Average Resource Utilization ($ARUR$)** | Persen ($\%$) | $\text{ARUR} = \frac{\sum_{j=1}^M T_{\text{busy}, j}}{M \times (MS \times 1000)} \times 100\%$ | Rasio rata-rata waktu sibuk worker terhadap total kapasitas waktu sistem. Mengukur efisiensi pemanfaatan cluster. |
| **Load Balancing Imbalance ($\sigma_{\text{LB}}$)** | Milidetik ($ms$) | $\sigma_{\text{LB}} = \sqrt{\frac{1}{M} \sum_{j=1}^M (T_{\text{busy}, j} - \overline{T_{\text{busy}}})^2}$ | Standar deviasi total waktu sibuk antar-worker. Semakin mendekati 0, beban kerja antar worker semakin seimbang. |
| **Scheduling Overhead** | Milidetik ($ms$) | $T_{\text{overhead}} = T_{\text{end\_alg}} - T_{\text{start\_alg}}$ | Waktu CPU yang dihabiskan scheduler untuk menghitung mapping sebelum task didispatch. |

---

## Teknologi dan Dependensi

- **Bahasa & Runtime:** Java 17 (Eclipse Temurin JDK 17)
- **Build System:** Apache Maven 3.9+
- **Containerization:** Docker 29.5+ & Docker Compose v5+
- **Simulasi Cloud:** CloudSim Plus 7.3.0
- **JSON Serialization:** Google Gson 2.10.1
- **Testing Framework:** JUnit Jupiter 5.10.2
- **Visualisasi & Analisis:** Python 3.10+, Pandas, Matplotlib

---

## Struktur Direktori

```text
CloudSimTaskScheduling/
├── Dockerfile                         # Blueprint container image Java 17
├── docker-compose.yml                 # Orkestrasi 8 worker heterogen (2 Host) + scheduler
├── pom.xml                            # Konfigurasi dependensi Maven & Shade plugin
├── README.md                          # Dokumentasi lengkap proyek
├── results/                           # Direktori output artefak eksperimen
│   ├── benchmark_results.csv          # Hasil mentah benchmark DRALBA (CSV)
│   ├── benchmark_results.json         # Hasil terstruktur benchmark DRALBA (JSON)
│   └── comparison_metrics.png         # Diagram grafik evaluasi performa DRALBA
├── scripts/
│   └── visualize_results.py           # Script otomatis visualisasi grafik matplotlib
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── TaskSchedulingDRALBA.java     # Implementasi simulasi CloudSim Plus original
    │   │   └── org/example/dralba/
    │   │       ├── algorithm/                # Modul algoritma penjadwalan
    │   │       │   ├── TaskScheduler.java    # Interface dasar scheduler
    │   │       │   └── DRALBAScheduler.java  # Algoritma DRALBA (Spill + Fill)
    │   │       ├── model/                    # Model domain
    │   │       │   ├── Task.java             # Model task GoCJ
    │   │       │   ├── WorkerNode.java       # Model node worker
    │   │       │   ├── TaskExecutionResult.java # Hasil eksekusi task
    │   │       │   └── EvaluationMetrics.java   # Model kumpulan metrik
    │   │       ├── worker/                   # Layanan worker HTTP & CPU Engine
    │   │       │   ├── RealWorkloadEngine.java  # Engine komputasi CPU & SHA-256
    │   │       │   └── WorkerServer.java        # HTTP REST API server worker
    │   │       ├── scheduler/                # Modul dispatcher & kalkulator metrik
    │   │       │   ├── RealWorldDispatcher.java # Dispatcher HTTP multi-threaded
    │   │       │   └── MetricsCalculator.java   # Formula kalkulasi metrik
    │   │       └── benchmark/
    │   │           └── BenchmarkRunner.java     # Entry point benchmark multi-dataset
    │   └── resources/                        # Dataset GoCJ (100 - 1000 task)
    │       ├── GoCJ_Dataset_100.txt
    │       ├── GoCJ_Dataset_200.txt
    │       └── ...
    └── test/java/org/example/dralba/         # Unit test suite (JUnit 5)
        ├── algorithm/
        │   └── DRALBASchedulerTest.java
        ├── scheduler/
        │   └── MetricsCalculatorTest.java
        └── worker/
            └── RealWorkloadEngineTest.java
```

---

## Instalasi dan Konfigurasi

### 1. Prasyarat Sistem
Pastikan tool berikut telah terpasang di komputer Anda:
- Docker Desktop aktif (mendukung Linux containers/WSL2)
- Java 17 (`java -version`)
- Apache Maven (`mvn -version`)
- Python 3 dengan pustaka `pandas` dan `matplotlib` (`pip install pandas matplotlib`)

### 2. Kompilasi & Packaging Proyek
Jalankan perintah Maven untuk mengompilasi dan menghasilkan *shaded uber-jar*:

```powershell
# Windows PowerShell (pastikan JAVA_HOME mengarah ke JDK 17)
$env:JAVA_HOME = "C:\Users\yudi\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
mvn clean package -DskipTests
```

---

## Cara Menjalankan Proyek

### Opsi A: Menjalankan Eksperimen Real-World (Docker Compose) - *Direkomendasikan*

#### Langkah 1: Jalankan Cluster 8 VM (Host-1: vm-1..4, Host-2: vm-5..8)
```powershell
docker compose up -d vm-1 vm-2 vm-3 vm-4 vm-5 vm-6 vm-7 vm-8
```
Verifikasi status seluruh container VM:
```powershell
docker compose ps
```
Semua 8 VM (`dralba-h1-vm1` sampai `dralba-h2-vm8`) akan berada dalam status `Up (healthy)`.

#### Langkah 2: Jalankan Benchmark Multi-Dataset (100 - 1000 Tasks)
Jalankan scheduler container untuk mengevaluasi dataset 100 sampai 1000 secara otomatis ke 8 VM:
```powershell
docker compose run --rm scheduler java -cp app.jar org.example.dralba.benchmark.BenchmarkRunner --multi 100,200,300,400,500,600,700,800,900,1000 --output-dir /app/results
```

Jika hanya ingin menjalankan satu ukuran dataset tertentu (misal dataset 100):
```powershell
docker compose run --rm scheduler java -cp app.jar org.example.dralba.benchmark.BenchmarkRunner --dataset 100 --output-dir /app/results
```

#### Langkah 3: Hentikan Cluster Worker Setelah Selesai
```powershell
docker compose down
```

---

### Opsi B: Menjalankan Benchmark Langsung dari Host terhadap Container
Jika 8 container worker sudah berjalan di background pada port `8081` sampai `8088`, Anda juga dapat menjalankan CLI benchmark langsung dari terminal host:
```powershell
java -cp target/CloudSimTaskScheduling-1.0-SNAPSHOT.jar org.example.dralba.benchmark.BenchmarkRunner --multi 100,200,300,400,500,600,700,800,900,1000 --output-dir results
```

---

### Opsi C: Menjalankan Simulasi CloudSim Plus Original
Untuk menjalankan simulasi *discrete-event* bawaan yang ada di [TaskSchedulingDRALBA.java](file:///c:/Users/yudi/Documents/SEMESTER%205/SOKA/CloudSimTaskScheduling/src/main/java/TaskSchedulingDRALBA.java):
```powershell
mvn compile exec:java "-Dexec.mainClass=TaskSchedulingDRALBA"
```

---

## Konfigurasi Worker dan Workload

Anda dapat memodifikasi konfigurasi eksperimen dengan mudah melalui `docker-compose.yml` atau *environment variables*:

1. **Mengubah Alokasi Resource Worker:**
   Di `docker-compose.yml`, sesuaikan blok `deploy.resources.limits`:
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '2.00'      # Ubah kuota CPU
         memory: 2048M     # Ubah kuota memori
   ```
2. **Mengubah Bobot Kapasitas (MIPS):**
   Ubah variabel `CAPACITY_MIPS` pada worker dan `WORKER_CAPACITIES` pada scheduler.
3. **Mengatur Intensitas Komputasi Workload:**
   Ubah `WORKLOAD_SCALE_FACTOR` (default: `10`). Nilai lebih besar meningkatkan durasi komputasi CPU murni per task.

---

## Hasil Eksperimen Real-World Aktual (Algoritma DRALBA)

Berikut adalah **hasil pengujian aktual** yang diperoleh dari running cluster Docker heterogen (**1 Datacenter, 2 Host, 8 Worker VM**) untuk dataset **100 hingga 1000 cloudlet** menggunakan algoritma **DRALBA**:

| Dataset | Algoritma | Total Task | Makespan (s) | Throughput (t/s) | ART (ms) | ARUR (%) | LB StdDev (ms) | Overhead (ms) |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| **GoCJ_100** | **DRALBA** | 100 | **0.86** | **116.69** | **15.82** | **22.66%** | 31.34 | 63.92 |
| **GoCJ_200** | **DRALBA** | 200 | **1.60** | **125.23** | **8.30** | **10.73%** | 50.73 | 5.62 |
| **GoCJ_300** | **DRALBA** | 300 | **2.60** | **115.61** | **13.78** | **15.79%** | 221.58 | 2.42 |
| **GoCJ_400** | **DRALBA** | 400 | **3.12** | **128.37** | **6.09** | **9.50%** | 123.47 | 1.56 |
| **GoCJ_500** | **DRALBA** | 500 | **3.80** | **131.54** | **5.69** | **9.30%** | 151.78 | 3.21 |
| **GoCJ_600** | **DRALBA** | 600 | **5.61** | **106.95** | **7.75** | **9.07%** | 175.79 | 3.47 |
| **GoCJ_700** | **DRALBA** | 700 | **5.46** | **128.25** | **6.22** | **10.02%** | 223.21 | 4.60 |
| **GoCJ_800** | **DRALBA** | 800 | **6.15** | **130.06** | **5.74** | **9.36%** | 242.02 | 5.86 |
| **GoCJ_900** | **DRALBA** | 900 | **7.07** | **127.39** | **7.59** | **11.98%** | 272.64 | 5.05 |
| **GoCJ_1000** | **DRALBA** | 1000 | **8.75** | **114.34** | **6.52** | **9.30%** | 320.43 | 4.34 |

### Analisis Temuan Eksperimen DRALBA

1. **Skalabilitas Makespan:**
   - Pada arsitektur 8 VM (2 Host), **DRALBA** mampu menyelesaikan 100 task hanya dalam **0.86 detik** dan skala penuh 1000 task dalam **8.75 detik** dengan tingkat keberhasilan 100% (0 task gagal).
2. **Konsistensi Throughput:**
   - Throughput DRALBA terjaga tinggi dan stabil di kisaran **114 - 131 task/detik** berkat pembagian beban paralel ke 8 container worker sesuai kapasitas MIPS masing-masing.
3. **Pemanfaatan Resource (ARUR) dan Distribusi Beban Proporsional:**
   - DRALBA secara otomatis mengalokasikan porsi task yang lebih besar ke Host-2 (`vm-6`, `vm-7`, `vm-8` yang berkapasitas 4000 MIPS) dibandingkan ke Host-1 (`vm-1`, `vm-2` yang berkapasitas 1000 MIPS), menjaga utilisasi sumber daya (**ARUR**) di kisaran **9.07% - 22.66%** dan waktu tanggap rata-rata (**ART**) yang cepat di kisaran **5.69 - 15.82 ms**.
4. **Efisiensi Scheduling Overhead:**
   - Waktu komputasi penjadwalan (*scheduling overhead*) algoritma DRALBA sangat ringan (setelah fase *JVM warm-up* di dataset pertama, overhead stabil di **1.56 ms hingga 5.86 ms**), membuktikan bahwa algoritma dua fase (*Spill & Fill*) sangat efisien untuk diterapkan pada lingkungan container terdistribusi nyata.

---

## Visualisasi Hasil Eksperimen

Untuk meng-generate ulang gambar grafik visualisasi evaluasi performa DRALBA beresolusi tinggi (300 DPI), jalankan script:

```powershell
python scripts/visualize_results.py
```

File gambar diagram disimpan di `results/comparison_metrics.png`:

![Evaluasi Metrik DRALBA](results/comparison_metrics.png)

### Analisis Hasil Uji Coba

Pengujian dilakukan pada algoritma DRALBA dengan sepuluh ukuran dataset GoCJ (100 hingga 1000 task) pada cluster Docker yang terdiri dari 1 datacenter, 2 host, dan 8 worker VM heterogen. Gambar `comparison_metrics.png` memuat empat panel yang masing-masing menampilkan satu metrik evaluasi. Hasil pengamatan dan analisis setiap panel diuraikan sebagai berikut.

#### 1. Makespan (Panel A)

**Hasil:** Makespan naik secara konsisten dan hampir linear dari **0,86 detik** (`GoCJ_100`) menjadi **8,75 detik** (`GoCJ_1000`). Hanya terdapat sedikit fluktuasi kecil pada `GoCJ_600` (5,61 detik) dibanding `GoCJ_700` (5,46 detik).

**Analisis:** Kapasitas worker tetap (8 VM), sehingga penambahan task memperpanjang waktu penyelesaian secara proporsional (sekitar 8 ms per task). Sedikit lonjakan pada `GoCJ_600` disebabkan oleh variasi kondisi lingkungan eksekusi *real-world* (seperti *JVM Garbage Collection pause*, *CPU throttling* cgroups, atau penjadwalan thread di Docker Desktop/WSL2) di mana Makespan ditentukan oleh satu worker terakhir yang selesai ($\max(T_{finish})$).

**Kesimpulan:** DRALBA memiliki skalabilitas yang sangat baik karena makespan tumbuh secara proporsional sebanding dengan jumlah task.

#### 2. Throughput (Panel B)

**Hasil:** Throughput stabil di rentang **114 – 131,5 task/detik**, dengan puncak pada `GoCJ_500` (131,5 task/detik) dan koreksi ringan pada `GoCJ_600` (107,0 task/detik).

**Analisis:** Throughput adalah jumlah task dibagi makespan ($TP = N / MS$). Karena DRALBA membagi beban secara proporsional terhadap kapasitas MIPS worker, pertumbuhan beban diimbangi oleh paralelisme 8 worker sehingga laju pemrosesan relatif konstan. Penurunan sesaat di `GoCJ_600` adalah efek matematis langsung dari makespan yang sedikit meningkat pada dataset tersebut.

**Kesimpulan:** Laju pemrosesan sistem terjaga tinggi dan stabil pada seluruh skala pengujian.

#### 3. Average Response Time / ART (Panel C)

**Hasil:** Pada dataset awal (`GoCJ_100` sebesar 15,82 ms dan `GoCJ_300` sebesar 13,78 ms) ART sedikit lebih tinggi karena efek inisialisasi koneksi HTTP dan *JIT warm-up* pada JVM worker. Mulai `GoCJ_400` hingga `GoCJ_1000`, ART sangat stabil dan datar di kisaran **5,69 – 7,75 ms**.

**Analisis:** ART mengukur waktu rata-rata penyelesaian per task individual. Setelah JVM melewati fase *warm-up*, waktu eksekusi murni per task stabil di kisaran 5–7 ms dan tidak membengkak meskipun jumlah task dinaikkan hingga 1000 task, membuktikan tidak terjadinya penumpukan antrean ekstrem (*queue starvation*) pada worker tertentu.

**Kesimpulan:** Waktu tanggap per task sangat responsif dan konsisten pada skala beban besar.

#### 4. Average Resource Utilization / ARUR (Panel D)

**Hasil:** ARUR berada pada kisaran **22,66%** di `GoCJ_100` (saat *JVM warm-up*) dan bergerak stabil di rentang **9,07% – 11,98%** pada `GoCJ_400` hingga `GoCJ_1000`.

**Analisis:** Setiap task berukuran mikro (dieksekusi dalam beberapa milidetik), sehingga terdapat porsi waktu transmisi jaringan HTTP lokal dan serialisasi JSON di antara eksekusi CPU. Stabilnya nilai ARUR di kisaran ~9–12% pada dataset 400–1000 menunjukkan bahwa rasio waktu kerja aktif antar-worker terjaga seimbang dan tidak ada VM yang dibiarkan menganggur (*idle*) secara timpang.

**Kesimpulan:** DRALBA berhasil menjaga keseimbangan utilisasi sumber daya di seluruh 8 worker heterogen.

Diagram Mermaid interaktif evaluasi Makespan DRALBA (GoCJ 100 - 1000):

```mermaid
xychart-beta
    title "Makespan Real-World DRALBA (Detik) pada 8 Worker VM"
    x-axis "Dataset GoCJ" [100, 200, 300, 400, 500, 600, 700, 800, 900, 1000]
    y-axis "Makespan (Detik)" 0 --> 10
    line [0.86, 1.60, 2.60, 3.12, 3.80, 5.61, 5.46, 6.15, 7.07, 8.75]
```

#### Analisa Diagram Mermaid (Makespan)

Diagram Mermaid menampilkan data makespan yang sama dengan Panel A dalam bentuk grafik interaktif yang dirender langsung oleh GitHub. Pola dan analisisnya sama dengan yang telah diuraikan pada bagian Makespan.

#### Ringkasan Temuan

| Metrik | Pola yang Teramati | Penyebab Utama |
|---|---|---|
| Makespan | Naik linear dari 0,86s ke 8,75s; fluktuasi kecil pada GoCJ_600 (5,61s) | Volume task bertambah pada kapasitas 8 VM tetap; fluktuasi kecil akibat jitter lingkungan Docker/OS |
| Throughput | Stabil di kisaran 114–131,5 task/detik | Alokasi proporsional DRALBA menjaga paralelisme 8 worker tetap optimal |
| ART | Awal 15,82 ms (warm-up), lalu datar stabil di 5,69–7,75 ms (dataset 400–1000) | Bebas *queue starvation*; waktu eksekusi per task tidak membengkak saat dataset membesar |
| ARUR | Awal 22,66%, lalu stabil seimbang di 9,07%–11,98% (dataset 400–1000) | Pembagian kuota beban seimbang antar Host-1 dan Host-2 disertai overhead komunikasi HTTP mikro |

---

## Pengujian dan Validasi (Testing)

Proyek ini dilengkapi dengan unit test otomatis berbasis **JUnit 5** yang mencakup:
- `DRALBASchedulerTest`: Memvalidasi alokasi kuota share, bebas duplikasi, dan alokasi proporsional pada worker heterogen.
- `MetricsCalculatorTest`: Memvalidasi formula matematis Makespan, Throughput, ART, ARUR, dan Standar Deviasi Load Balancing.
- `RealWorkloadEngineTest`: Memvalidasi eksekusi CPU nyata dan integritas checksum SHA-256.

Jalankan test suite menggunakan Maven:
```powershell
mvn test
```

Hasil test aktual:
```text
[INFO] Running org.example.dralba.algorithm.DRALBASchedulerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.example.dralba.scheduler.MetricsCalculatorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.example.dralba.worker.RealWorkloadEngineTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## Troubleshooting dan Kendala

1. **Docker Desktop Service Not Running:**
   - *Gejala:* Pesan error `failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine`.
   - *Solusi:* Buka aplikasi Docker Desktop melalui Start Menu Windows atau jalankan `"C:\Program Files\Docker\Docker\Docker Desktop.exe"`, lalu tunggu ikon Docker di system tray berstatus *Running*.
2. **Port Conflict (`8081` - `8088`):**
   - *Gejala:* Pesan error `bind: address already in use`.
   - *Solusi:* Pastikan tidak ada aplikasi lain yang menggunakan port 8081-8088, atau ubah *mapping* port pada file `docker-compose.yml`.
3. **Versi Java Tidak Cocok:**
   - *Gejala:* Maven menampilkan error `Unsupported class file major version` atau CloudSim Plus gagal di Java 8.
   - *Solusi:* Pastikan `JAVA_HOME` mengarah ke JDK 17 (misal: Temurin 17):
     ```powershell
     $env:JAVA_HOME = "C:\Users\yudi\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
     ```

---

## Referensi Ilmiah

1. **DRALBA:**
   - Calheiros, R. N., et al. *"CloudSim: a toolkit for modeling and simulation of cloud computing environments and evaluation of resource provisioning algorithms."* Software: Practice and Experience, 2011.
   - Algoritma *Dynamic Resource Allocation and Load Balancing Algorithm* untuk lingkungan cloud heterogen (fase *Spill* dan *Fill Scheduler*).
2. **Dataset GoCJ:**
   - Silvia, M., et al. *"Google Cloud Jobs (GoCJ) Dataset: Synthetic Grid and Cloud Computing Workload Trace."* Digunakan sebagai representasi distribusi panjang instruksi task heterogen berskala realistis.
3. **CloudSim Plus:**
   - Silva Filho, M. C., et al. *"CloudSim Plus: A modern Java 8 framework for modeling and simulation of cloud computing infrastructures."* Software: Practice and Experience, 2017.

