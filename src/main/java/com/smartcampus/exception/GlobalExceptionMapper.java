package com.smartcampus.exception;

import com.smartcampus.model.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global "Safety Net" Exception Mapper — catches ALL unhandled Throwables.
 *
 * This mapper is the last line of defense. Any runtime exception not caught by
 * a more specific mapper (e.g., NullPointerException, IndexOutOfBoundsException)
 * is intercepted here, ensuring that:
 *   1. The client NEVER sees a raw Java stack trace or default HTML error page.
 *   2. The response is always a clean, structured JSON body with HTTP 500.
 *   3. The internal exception details are safely logged server-side only.
 *
 * Security Note: Exposing stack traces to external clients is a security risk.
 * A trace can reveal: internal package structure, class names, library versions
 * (which may have known CVEs), server file paths, and the application's internal
 * logic — all of which can be leveraged by an attacker to plan targeted exploits.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Log the full stack trace on the server (safe, internal)
        LOGGER.log(Level.SEVERE, "Unhandled exception intercepted by GlobalExceptionMapper", exception);

        // Return a sanitised, generic error to the client (no internal details leaked)
        ApiError error = new ApiError(
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "Internal Server Error",
            "An unexpected error occurred. Please contact the system administrator."
        );
        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.APPLICATION_JSON)
            .entity(error)
            .build();
    }
}
