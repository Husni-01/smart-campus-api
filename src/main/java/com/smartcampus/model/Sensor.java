package com.smartcampus.model;

/**
 * Represents a sensor deployed within a campus room.
 * Sensors record environmental metrics and have a status lifecycle.
 */
public class Sensor {

    /** Unique identifier, e.g., "TEMP-001" */
    private String id;

    /** Category, e.g., "Temperature", "Occupancy", "CO2" */
    private String type;

    /** Current state: "ACTIVE", "MAINTENANCE", or "OFFLINE" */
    private String status;

    /** The most recent measurement recorded by the sensor */
    private double currentValue;

    /** Foreign key linking to the Room where the sensor is located */
    private String roomId;

    // --- Constructors ---

    public Sensor() {}

    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
