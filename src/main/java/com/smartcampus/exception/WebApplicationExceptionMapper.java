package com.smartcampus.exception;

import com.smartcampus.model.ApiError;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

/**
 * Maps all built-in JAX-RS WebApplicationExceptions (NotFoundException,
 * BadRequestException, ClientErrorException, etc.) to clean JSON responses.
 *
 * Without this, the GlobalExceptionMapper<Throwable> would catch these and
 * return a generic 500. This mapper takes priority for any WebApplicationException
 * and preserves the correct HTTP status code while ensuring the response is JSON.
 *
 * Examples handled:
 *  - NotFoundException       → 404
 *  - BadRequestException     → 400
 *  - ClientErrorException    → 409 (or whatever status was set)
 *  - NotAuthorizedException  → 401
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOGGER = Logger.getLogger(WebApplicationExceptionMapper.class.getName());

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse().getStatus();
        String reason = Response.Status.fromStatusCode(status) != null
            ? Response.Status.fromStatusCode(status).getReasonPhrase()
            : "HTTP Error";

        LOGGER.warning(String.format("WebApplicationException [%d]: %s", status, exception.getMessage()));

        ApiError error = new ApiError(
            status,
            reason,
            exception.getMessage() != null
                ? exception.getMessage()
                : reason
        );
        return Response
            .status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(error)
            .build();
    }
}
