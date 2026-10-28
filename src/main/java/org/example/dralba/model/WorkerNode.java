package org.example.dralba.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Merepresentasikan node worker heterogen (container Docker).
 */
public class WorkerNode implements Serializable {
    private final String id;
    private final String name;
    private final String endpointUrl;
    private final double capacityMips; // Kapasitas komputasi relatif / MIPS
    private final int cores;
    private final String hostId;
    private final String datacenterId;

    public WorkerNode(String id, String name, String endpointUrl, double capacityMips, int cores) {
        this(id, name, endpointUrl, capacityMips, cores, "host-1", "datacenter-1");
    }

    public WorkerNode(String id, String name, String endpointUrl, double capacityMips, int cores, String hostId, String datacenterId) {
        this.id = id;
        this.name = name;
        this.endpointUrl = endpointUrl;
        this.capacityMips = capacityMips;
        this.cores = cores;
        this.hostId = (hostId != null && !hostId.isBlank()) ? hostId : "host-1";
        this.datacenterId = (datacenterId != null && !datacenterId.isBlank()) ? datacenterId : "datacenter-1";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public double getCapacityMips() {
        return capacityMips;
    }

    public int getCores() {
        return cores;
    }

    public String getHostId() {
        return hostId;
    }

    public String getDatacenterId() {
        return datacenterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerNode that = (WorkerNode) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WorkerNode{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", hostId='" + hostId + '\'' +
                ", datacenterId='" + datacenterId + '\'' +
                ", endpointUrl='" + endpointUrl + '\'' +
                ", capacityMips=" + capacityMips +
                ", cores=" + cores +
                '}';
    }
}

