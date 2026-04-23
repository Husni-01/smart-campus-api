package com.smartcampus.repository;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, in-memory data store acting as the single source of truth for the API.
 *
 * This class is implemented as a Singleton. Since JAX-RS resources are instantiated
 * per-request by default, shared state cannot be stored as instance variables on the
 * resource classes. Instead, all data is centralized here using ConcurrentHashMap,
 * which provides thread-safe read/write operations without requiring explicit
 * synchronization blocks for most use cases.
 *
 * Pre-populated with sample data to aid testing and video demonstration.
 */
public class InMemoryDataStore {

    // --- Singleton Implementation ---
    private static final InMemoryDataStore INSTANCE = new InMemoryDataStore();

    public static InMemoryDataStore getInstance() {
        return INSTANCE;
    }

    // --- In-Memory Data Structures ---

    /** Map of roomId -> Room */
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    /** Map of sensorId -> Sensor */
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    /**
     * Map of sensorId -> List of SensorReadings.
     * ConcurrentHashMap ensures thread-safe access to the top-level map.
     * Individual lists are wrapped in synchronized lists for safe concurrent appends.
     */
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // --- Pre-populated Sample Data ---
    private InMemoryDataStore() {
        // Seed Rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-105", "Computer Science Lab", 30);
        Room r3 = new Room("HALL-001", "Main Assembly Hall", 500);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);

        // Seed Sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 410.0, "LAB-105");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "LIB-301");
        Sensor s4 = new Sensor("TEMP-002", "Temperature", "ACTIVE", 19.8, "LAB-105");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);
        sensors.put(s4.getId(), s4);

        // Link sensors to rooms
        r1.getSensorIds().add("TEMP-001");
        r1.getSensorIds().add("OCC-001");
        r2.getSensorIds().add("CO2-001");
        r2.getSensorIds().add("TEMP-002");

        // Seed Readings
        readings.put("TEMP-001", new ArrayList<>());
        readings.put("CO2-001", new ArrayList<>());
        readings.put("OCC-001", new ArrayList<>());
        readings.put("TEMP-002", new ArrayList<>());
    }

    // --- Room Operations ---

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Room getRoomById(String id) {
        return rooms.get(id);
    }

    public void addRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public boolean removeRoom(String id) {
        return rooms.remove(id) != null;
    }

    public boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

    // --- Sensor Operations ---

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Sensor getSensorById(String id) {
        return sensors.get(id);
    }

    public void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        // Initialise an empty reading list for the new sensor
        readings.putIfAbsent(sensor.getId(), new ArrayList<>());
    }

    public boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }

    // --- SensorReading Operations ---

    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.getOrDefault(sensorId, new ArrayList<>());
    }

    public synchronized void addReading(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }
}
