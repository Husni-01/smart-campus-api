package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.repository.InMemoryDataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * SensorReading Sub-Resource — manages historical readings for a specific sensor.
 *
 * This class is NOT annotated with @Path at the class level; it is a sub-resource
 * reached via the locator method in SensorResource. JAX-RS dynamically dispatches
 * to this class after the parent locator resolves it, passing along the sensorId context.
 *
 * This pattern (Sub-Resource Locator) cleanly separates reading management logic from
 * sensor management logic, avoiding a single bloated controller class.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final InMemoryDataStore store = InMemoryDataStore.getInstance();

    /**
     * Constructor called by SensorResource's sub-resource locator.
     * The sensorId context is injected at construction time.
     */
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns the full historical reading log for the specified sensor.
     */
    @GET
    public Response getReadings() {
        List<SensorReading> readingList = store.getReadingsForSensor(sensorId);
        return Response.ok(readingList).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Appends a new reading to the sensor's history.
     *
     * Business Rule: Sensors in "MAINTENANCE" status are physically disconnected
     * and cannot accept new readings. Throws SensorUnavailableException (403 Forbidden).
     *
     * Side Effect: A successful POST also updates the parent Sensor's currentValue
     * to ensure data consistency across the API.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensorById(sensorId);

        // State Constraint: block readings from sensors under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is currently under MAINTENANCE "
                + "and cannot accept new readings."
            );
        }

        if ("OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is OFFLINE and cannot accept new readings."
            );
        }

        // Assign a unique ID and capture current timestamp if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Persist the reading
        store.addReading(sensorId, reading);

        // Side effect: keep parent sensor's currentValue in sync
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
