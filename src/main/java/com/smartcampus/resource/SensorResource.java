package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.repository.InMemoryDataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sensor Resource — manages the /api/v1/sensors collection.
 *
 * Also acts as a Sub-Resource Locator for /api/v1/sensors/{sensorId}/readings,
 * delegating to SensorReadingResource for historical data management.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final InMemoryDataStore store = InMemoryDataStore.getInstance();

    /**
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=CO2
     *
     * Returns all sensors. If the optional "type" query parameter is provided,
     * filters the list to only return sensors of that category.
     *
     * Using @QueryParam is superior to path-based type (e.g., /sensors/type/CO2)
     * because it keeps the base resource URI clean, allows combining multiple
     * filters naturally (e.g., ?type=CO2&status=ACTIVE), and semantically
     * signals a search/filter rather than a distinct sub-resource.
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensorList = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.isBlank()) {
            sensorList = sensorList.stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
        }

        return Response.ok(sensorList).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     * Returns detailed metadata for a specific sensor.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensorById(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        return Response.ok(sensor).build();
    }

    /**
     * POST /api/v1/sensors
     * Registers a new sensor.
     *
     * @Consumes(MediaType.APPLICATION_JSON) ensures JAX-RS will reject any
     * request with a Content-Type other than application/json (e.g., text/plain
     * or application/xml) with a 415 Unsupported Media Type error, before the
     * method body is even reached.
     *
     * Business Rule: The roomId specified in the body MUST refer to an existing room.
     * If not, throws LinkedResourceNotFoundException (422 Unprocessable Entity).
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            throw new BadRequestException("Sensor body must include a valid 'id' field.");
        }
        if (store.sensorExists(sensor.getId())) {
            throw new ClientErrorException(
                "A sensor with ID '" + sensor.getId() + "' already exists.",
                Response.Status.CONFLICT
            );
        }

        // Validate that the referenced room actually exists
        if (sensor.getRoomId() == null || !store.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Cannot create sensor: Room with ID '" + sensor.getRoomId()
                + "' does not exist in the system."
            );
        }

        store.addSensor(sensor);

        // Maintain the bidirectional link: add sensor ID to its room's sensorIds list
        Room room = store.getRoomById(sensor.getRoomId());
        if (room != null && !room.getSensorIds().contains(sensor.getId())) {
            room.getSensorIds().add(sensor.getId());
        }

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    /**
     * Sub-Resource Locator for /api/v1/sensors/{sensorId}/readings
     *
     * Instead of defining all reading paths in this class (which would create a
     * bloated "god class"), this method delegates to a dedicated SensorReadingResource.
     * JAX-RS discovers and invokes the appropriate method on the returned instance.
     * This pattern improves separation of concerns, testability, and maintainability.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensorById(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        // Inject sensorId context into the sub-resource
        return new SensorReadingResource(sensorId);
    }
}
