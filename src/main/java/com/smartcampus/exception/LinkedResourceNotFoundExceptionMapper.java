package com.smartcampus.exception;

import com.smartcampus.model.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 *
 * Scenario: A client POSTs a new Sensor with a roomId that does not exist.
 * 422 is semantically more accurate than 404 here because the REQUEST URL is valid
 * and was found — the problem is that the DATA INSIDE the valid payload contains
 * an invalid reference. 404 implies the endpoint itself was not found, which is
 * misleading. 422 precisely communicates a semantic/business logic validation failure.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        ApiError error = new ApiError(
            422,
            "Unprocessable Entity",
            exception.getMessage()
        );
        return Response
            .status(422)
            .type(MediaType.APPLICATION_JSON)
            .entity(error)
            .build();
    }
}
