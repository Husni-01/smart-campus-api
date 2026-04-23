package com.smartcampus.exception;

import com.smartcampus.model.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps RoomNotEmptyException to HTTP 409 Conflict.
 *
 * Scenario: A client attempts to DELETE a room that still has active sensors.
 * Returning 409 Conflict communicates that the request is valid but conflicts
 * with the current state of the resource.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        ApiError error = new ApiError(
            Response.Status.CONFLICT.getStatusCode(),
            "Room Conflict",
            exception.getMessage()
        );
        return Response
            .status(Response.Status.CONFLICT)
            .type(MediaType.APPLICATION_JSON)
            .entity(error)
            .build();
    }
}
