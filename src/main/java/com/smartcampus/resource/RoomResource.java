package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.repository.InMemoryDataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Room Resource — manages the /api/v1/rooms collection.
 *
 * JAX-RS creates a new instance of this class for every incoming HTTP request
 * (per-request lifecycle). Therefore, shared state is never stored here; it
 * is always retrieved from the singleton InMemoryDataStore.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final InMemoryDataStore store = InMemoryDataStore.getInstance();

    /**
     * GET /api/v1/rooms
     * Returns a list of all rooms in the system.
     */
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());
        return Response.ok(roomList).build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Returns detailed metadata for a specific room.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRoomById(roomId);
        if (room == null) {
            throw new NotFoundException("Room not found with ID: " + roomId);
        }
        return Response.ok(room).build();
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room. Returns 201 Created with the new room in the body.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            throw new BadRequestException("Room body must include a valid 'id' field.");
        }
        if (store.roomExists(room.getId())) {
            throw new ClientErrorException(
                "A room with ID '" + room.getId() + "' already exists.",
                Response.Status.CONFLICT
            );
        }
        store.addRoom(room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     * Decommissions a room. A room cannot be deleted if it still has sensors assigned.
     *
     * Idempotency: The first successful DELETE removes the room (204 No Content).
     * Subsequent identical DELETE requests return 404 Not Found, since the resource
     * no longer exists. Both outcomes result in the same application state (room absent),
     * satisfying REST idempotency semantics.
     *
     * Business Rule: Throws RoomNotEmptyException (409) if sensors are still linked.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoomById(roomId);
        if (room == null) {
            throw new NotFoundException("Room not found with ID: " + roomId);
        }

        // Business logic constraint: prevent orphaned sensors
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Room '" + roomId + "' cannot be deleted. It still has "
                + room.getSensorIds().size() + " active sensor(s) assigned: "
                + room.getSensorIds()
            );
        }

        store.removeRoom(roomId);
        return Response.noContent().build();
    }
}
