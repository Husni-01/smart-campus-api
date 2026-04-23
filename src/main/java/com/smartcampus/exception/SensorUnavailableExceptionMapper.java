package com.smartcampus.exception;

import com.smartcampus.model.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps SensorUnavailableException to HTTP 403 Forbidden.
 *
 * Scenario: A client attempts to POST a new reading to a sensor that is currently
 * in "MAINTENANCE" status. The server understands the request but refuses to
 * fulfill it due to the sensor's current operational state.
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        ApiError error = new ApiError(
            Response.Status.FORBIDDEN.getStatusCode(),
            "Sensor Unavailable",
            exception.getMessage()
        );
        return Response
            .status(Response.Status.FORBIDDEN)
            .type(MediaType.APPLICATION_JSON)
            .entity(error)
            .build();
    }
}
