package org.ujorm.petstore;

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.ujorm.petstore.Constants.Url;
import org.ujorm.petstore.utilities.Bootstrap;

/**
 * Entry point of the self-contained executable, built by GraalVM into a native binary.
 * <p>
 * The WAR deployment discovers {@link Bootstrap} and the servlets by scanning the
 * {@code @WebListener} and {@code @WebServlet} annotations. A native image can not scan
 * anything at runtime, so the very same components are registered programmatically here.
 * No class of the application is modified for that.
 *
 * @see org.ujorm.core.generator.HandlerPrecompiler The build step that removes the last
 *      need of a Java compiler at runtime.
 */
public final class Main {

    /** Directory of the static resources on the class path. */
    private static final String RESOURCE_BASE = "META-INF/resources";

    /** The port of the HTTP connector, overridable by the first argument or the PORT variable. */
    private static final int DEFAULT_PORT = 8080;

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        var port = resolvePort(args);
        var server = new Server(port);
        server.setHandler(createHandler(server));
        server.start();

        System.out.printf("Ujorm PetStore is listening on http://localhost:%s/%n", port);
        server.join();
    }

    /** Builds the servlet context with the same components the WAR gets by the annotation scanning. */
    private static ServletContextHandler createHandler(Server server) {
        var handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.setBaseResource(ResourceFactory.of(server).newClassLoaderResource(RESOURCE_BASE));

        // The listener opens the Avaje scope and creates the database schema
        handler.addEventListener(new Bootstrap());

        // The patterns are taken from Constants.Url, the same source the @WebServlet annotations use.
        // The empty pattern of PetServlet is an exact match of the context root, while the default
        // servlet on "/" keeps serving the static resources - exactly as in the WAR deployment.
        var defaultServlet = new ServletHolder("default", new DefaultServlet());
        defaultServlet.setInitParameter("dirAllowed", "false"); // no directory listing of the resources
        handler.addServlet(defaultServlet, "/");
        handler.addServlet(new ServletHolder("pet", new PetServlet()), Url.PETS);
        handler.addServlet(new ServletHolder("info", new InfoServlet()), Url.INFO);

        return handler;
    }

    /** Resolves the port from the first argument, the PORT variable or the default. */
    private static int resolvePort(String[] args) {
        var value = args.length > 0 ? args[0] : System.getenv("PORT");
        return value != null && !value.isBlank() ? Integer.parseInt(value.trim()) : DEFAULT_PORT;
    }
}
