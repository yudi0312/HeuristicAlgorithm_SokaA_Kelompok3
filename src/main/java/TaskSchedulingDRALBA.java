import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

public class TaskSchedulingDRALBA {
    public static void main(String[] args) {
        CloudSim simulation = new CloudSim();

        // 1. MEMBANGUN DATACENTER & HOST
        Host host1 = createHost(4, 8192, 500000, 10000);
        Host host2 = createHost(8, 16384, 1000000, 10000);
        Host host3 = createHost(4, 16384, 1000000, 10000);
        Host host4 = createHost(8, 32768, 2000000, 10000);

        List<Host> hostList = List.of(host1, host2, host3, host4);
        new DatacenterSimple(simulation, hostList);

        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);
        List<Vm> vmList = new ArrayList<>();

        // 2. MEMBUAT 8 VM HETEROGEN
        vmList.add(new VmSimple(1000, 1).setRam(2048));
        vmList.add(new VmSimple(1000, 1).setRam(2048));
        vmList.add(new VmSimple(2000, 2).setRam(4096));
        vmList.add(new VmSimple(3000, 2).setRam(8192));
        vmList.add(new VmSimple(2500, 2).setRam(4096));
        vmList.add(new VmSimple(4000, 4).setRam(8192));
        vmList.add(new VmSimple(4000, 4).setRam(8192));
        vmList.add(new VmSimple(4000, 4).setRam(8192));
        broker.submitVmList(vmList);

        // 3. MEMBUAT DATASET CLOUDLET DARI FILE TXT GOCJ
        String datasetPath = "src/main/resources/GoCJ_Dataset_500.txt";
        List<Cloudlet> cloudletList = createCloudletsFromGoCJ(datasetPath);

        // --- IMPLEMENTASI ALGORITMA DRALBA ---
        double totalMips = vmList.stream().mapToDouble(Vm::getMips).sum();
        double totalLength = cloudletList.stream().mapToDouble(Cloudlet::getLength).sum();

        Map<Vm, Double> vmShareMap = new HashMap<>();
        Map<Vm, Double> vmEftMap = new HashMap<>();
        for (Vm vm : vmList) {
            double vmComputRatio = vm.getMips() / totalMips;
            vmShareMap.put(vm, totalLength * vmComputRatio);
            vmEftMap.put(vm, 0.0);
        }

        List<Cloudlet> unmappedTasks = new ArrayList<>(cloudletList);
        unmappedTasks.sort(Comparator.comparingDouble(Cloudlet::getLength).reversed());

        // Fase Spill Scheduler
        while (!unmappedTasks.isEmpty()) {
            Vm maxShareVm = vmShareMap.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
            double maxShare = vmShareMap.get(maxShareVm);

            Cloudlet lpTask = null;
            for (Cloudlet task : unmappedTasks) {
                if (task.getLength() <= maxShare) {
                    lpTask = task;
                    break;
                }
            }

            if (lpTask != null) {
                broker.bindCloudletToVm(lpTask, maxShareVm);
                vmShareMap.put(maxShareVm, maxShare - lpTask.getLength());
                vmEftMap.put(maxShareVm, vmEftMap.get(maxShareVm) + (lpTask.getLength() / maxShareVm.getMips()));
                unmappedTasks.remove(lpTask);
            } else {
                break;
            }
        }

        // Fase Fill Scheduler
        for (Cloudlet task : unmappedTasks) {
            Vm eftVm = vmEftMap.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey();
            broker.bindCloudletToVm(task, eftVm);
            vmEftMap.put(eftVm, vmEftMap.get(eftVm) + (task.getLength() / eftVm.getMips()));
        }

        broker.submitCloudletList(cloudletList);

        // 4. JALANKAN SIMULASI
        simulation.start();

        List<Cloudlet> finishedList = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedList).build();

        // 5. KALKULASI 5 METRIK UTAMA
        System.out.println("\n========== HASIL EVALUASI METRIK DRALBA ==========");

        // 1. Makespan (Waktu penyelesaian tugas terakhir)
        double makespan = finishedList.stream().mapToDouble(Cloudlet::getFinishTime).max().orElse(0.0);
        System.out.printf("1. Makespan               : %.2f Detik\n", makespan);

        // 2. Average Waiting Time
        double totalWaitTime = finishedList.stream().mapToDouble(Cloudlet::getWaitingTime).sum();
        double avgWaitTime = totalWaitTime / finishedList.size();
        System.out.printf("2. Average Waiting Time   : %.2f Detik\n", avgWaitTime);

        // 3. Kalkulasi Waktu Kerja VM (Menggunakan Max Finish Time per VM)
        Map<Vm, Double> vmWorkingTimes = new HashMap<>();

        for (Cloudlet c : finishedList) {

            double currentMax = vmWorkingTimes.getOrDefault(c.getVm(), 0.0);
            if (c.getFinishTime() > currentMax) {
                vmWorkingTimes.put(c.getVm(), c.getFinishTime());
            }
        }

        double trueTotalExecTime = 0;
        for (Double time : vmWorkingTimes.values()) {
            trueTotalExecTime += time;
        }

        // Utilisasi = Total Waktu Kerja / (Makespan * Jumlah VM)
        double resourceUtilization = (trueTotalExecTime / (makespan * vmList.size())) * 100;
        System.out.printf("3. Resource Utilization   : %.2f %%\n", resourceUtilization);

        // 4. Load Balancing (Standar Deviasi waktu kerja antar VM)
        double avgVmTime = trueTotalExecTime / vmList.size();
        double sumSquaredDiffs = 0;

        for (Vm vm : vmList) {
            double time = vmWorkingTimes.getOrDefault(vm, 0.0);
            sumSquaredDiffs += Math.pow(time - avgVmTime, 2);
        }

        double loadBalancingStdDev = Math.sqrt(sumSquaredDiffs / vmList.size());
        System.out.printf("4. Load Balancing (StdDev): %.2f (Semakin mendekati 0 semakin seimbang)\n", loadBalancingStdDev);

        // 5. Energy Consumption (Estimasi konsumsi daya berdasarkan utilisasi dan Makespan)
        // Asumsi rata-rata server mengkonsumsi 250 Watt saat aktif
        double powerPerHostWatt = 250.0;
        double totalEnergyWattSec = makespan * hostList.size() * powerPerHostWatt;
        double totalEnergyKWh = totalEnergyWattSec / 3600000.0; // Konversi ke kWh
        System.out.printf("5. Energy Consumption     : %.6f kWh (Estimasi Konsumsi Total)\n", totalEnergyKWh);
        System.out.println("==================================================\n");
    }

    private static Host createHost(int pes, long ram, long storage, long bw) {
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < pes; i++) { peList.add(new PeSimple(4000)); }
        return new HostSimple(ram, bw, storage, peList);
    }

    private static List<Cloudlet> createCloudletsFromGoCJ(String filePath) {
        List<Cloudlet> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    long length = Long.parseLong(line);
                    list.add(new CloudletSimple(length, 1));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Gagal membaca dataset: " + e.getMessage());
        }
        return list;
    }
}