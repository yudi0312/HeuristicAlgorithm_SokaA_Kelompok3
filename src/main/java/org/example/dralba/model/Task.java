package org.example.dralba.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Merepresentasikan task komputasi yang diambil dari dataset GoCJ.
 */
public class Task implements Serializable {
    private final int id;
    private final long length; // Panjang instruksi / workload weight (MI)

    public Task(int id, long length) {
        this.id = id;
        this.length = length;
    }

    public int getId() {
        return id;
    }

    public long getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", length=" + length +
                '}';
    }
}

