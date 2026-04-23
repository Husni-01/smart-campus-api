package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root Discovery Endpoint.
 *
 * Provides essential API metadata at GET /api/v1, including versioning,
 * contact details, and navigational links to primary resource collections.
 * This implements the HATEOAS (Hypermedia as the Engine of Application State)
 * principle by embedding resource URIs directly in the response.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", "Smart Campus API");
        response.put("version", "1.0.0");
        response.put("description", "RESTful API for campus room and sensor management");
        response.put("contact", "admin@smartcampus.ac.uk");
        response.put("status", "operational");

        // HATEOAS: Embed navigational links to guide clients to primary collections
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", "/api/v1");
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("links", links);

        return Response.ok(response).build();
    }
}
