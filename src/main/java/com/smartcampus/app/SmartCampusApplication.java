package com.smartcampus.app;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * JAX-RS Application Configuration.
 *
 * The @ApplicationPath annotation designates "/api/v1" as the versioned
 * entry point for the API, fulfilling the JAX-RS specification requirement.
 * ResourceConfig extends javax.ws.rs.core.Application and provides a
 * fluent API for programmatic configuration in Jersey.
 *
 * All classes under the "com.smartcampus" package (resources, filters,
 * exception mappers) are automatically discovered via package scanning.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Auto-scan all JAX-RS components in the project package
        packages("com.smartcampus");
        // Register Jackson for automatic JSON serialization/deserialization
        register(JacksonFeature.class);
    }
}
