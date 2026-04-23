package com.smartcampus;

import com.smartcampus.app.SmartCampusApplication;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.File;
import java.util.logging.Logger;

/**
 * Application entry point.
 * Bootstraps the embedded Tomcat server and registers the Jersey JAX-RS servlet.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int PORT = 8080;
    private static final String BASE_PATH = "/api/v1/*";

    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(PORT);

        // Required in Tomcat 9+ to create a default connector
        tomcat.getConnector();

        // Add a context with an empty path (root)
        Context ctx = tomcat.addContext("", new File(".").getAbsolutePath());

        // Register Jersey servlet with our JAX-RS application
        Tomcat.addServlet(ctx, "jersey", new ServletContainer(new SmartCampusApplication()));
        ctx.addServletMappingDecoded(BASE_PATH, "jersey");

        tomcat.start();
        LOGGER.info("=========================================================");
        LOGGER.info("  Smart Campus API started successfully!");
        LOGGER.info("  Base URL : http://localhost:" + PORT + "/api/v1");
        LOGGER.info("  Rooms    : http://localhost:" + PORT + "/api/v1/rooms");
        LOGGER.info("  Sensors  : http://localhost:" + PORT + "/api/v1/sensors");
        LOGGER.info("=========================================================");

        // Block the main thread to keep the server running
        tomcat.getServer().await();
    }
}
