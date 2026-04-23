package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * API Request and Response Logging Filter.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter to provide
 * observability for every HTTP transaction without modifying any resource method.
 *
 * Advantage of filter-based logging over manual Logger.info() in each resource:
 *  - Single point of maintenance: adding/changing logging logic requires editing
 *    only this one class, not dozens of resource methods.
 *  - Guaranteed coverage: new endpoints are automatically logged without any
 *    developer intervention, eliminating the risk of accidentally skipping logging.
 *  - Separation of concerns: resource classes focus solely on business logic;
 *    cross-cutting concerns like logging, auth, and CORS belong in filters.
 *  - Consistency: all requests are logged in an identical format.
 *
 * The @Provider annotation ensures Jersey auto-discovers this filter during
 * package scanning and registers it for all requests/responses.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Invoked on every incoming request BEFORE the resource method is called.
     * Logs the HTTP method and request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format(
            "[REQUEST]  --> %s %s",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri()
        ));
    }

    /**
     * Invoked on every outgoing response AFTER the resource method returns.
     * Logs the final HTTP status code.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format(
            "[RESPONSE] <-- %d %s  (for %s %s)",
            responseContext.getStatus(),
            responseContext.getStatusInfo().getReasonPhrase(),
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri()
        ));
    }
}
