package com.smartcampus.exception;

import com.smartcampus.model.ApiError;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps JAX-RS NotFoundException to HTTP 404 Not Found.
 *
 * Without this explicit mapper, the GlobalExceptionMapper<Throwable> would catch
 * NotFoundException and return a generic 500, which is incorrect. This mapper
 * takes priority and ensures 404 is returned with a clean JSON body.
 */
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        ApiError error = new ApiError(
            Response.Status.NOT_FOUND.getStatusCode(),
            "Not Found",
            exception.getMessage() != null
                ? exception.getMessage()
                : "The requested resource could not be found."
        );
        return Response
            .status(Response.Status.NOT_FOUND)
            .type(MediaType.APPLICATION_JSON)
            .entity(error)
            .build();
    }
}
