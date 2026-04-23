package com.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a physical room on campus.
 * Rooms are the top-level container resource and may contain multiple sensors.
 */
public class Room {

    /** Unique identifier, e.g., "LIB-301" */
    private String id;

    /** Human-readable name, e.g., "Library Quiet Study" */
    private String name;

    /** Maximum occupancy for safety regulations */
    private int capacity;

    /** Collection of IDs of sensors deployed in this room */
    private List<String> sensorIds = new ArrayList<>();

    // --- Constructors ---

    public Room() {}

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getSensorIds() {
        return sensorIds;
    }

    public void setSensorIds(List<String> sensorIds) {
        this.sensorIds = sensorIds;
    }
}
