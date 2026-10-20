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

    public WorkerNode(String id, String name, String endpointUrl, double capacityMips, int cores) {
        this.id = id;
        this.name = name;
        this.endpointUrl = endpointUrl;
        this.capacityMips = capacityMips;
        this.cores = cores;
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
                ", endpointUrl='" + endpointUrl + '\'' +
                ", capacityMips=" + capacityMips +
                ", cores=" + cores +
                '}';
    }
}

