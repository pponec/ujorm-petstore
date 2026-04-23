package org.ujorm.petstore.utilities;

import io.avaje.inject.BeanScope;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ujorm.petstore.DatabaseInitializer;
import org.ujorm.petstore.LoginServlet;
import org.ujorm.petstore.PetServlet;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.logging.LogManager;

/**
 * Unified application bootstrap.
 * Handles programmatic registration of Servlets, Filters, DB Schema, and graceful shutdown.
 */
@WebListener
public class Bootstrap implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);
    private static BeanScope beanScope;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 1. Load logging configuration as the very first step to ensure ISO formatting
        loadLoggingConfiguration();

        LOGGER.info("Initializing Ujorm PetStore ecosystem...");
        var ctx = sce.getServletContext();

        // 2. Initialize Avaje DI container
        beanScope = BeanScope.builder().build();

        // 3. Initialize Database Schema
        initSchema();

        // 4. Register Filters
        registerFilter(ctx, TransactionFilter.class);

        // 5. Register Servlets
        registerServlet(ctx, beanScope.get(PetServlet.class));
        registerServlet(ctx, beanScope.get(LoginServlet.class));

        LOGGER.info("Initialization complete.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (beanScope != null) {
            beanScope.close();
            LOGGER.info("Avaje BeanScope closed. Resources released.");
        }
    }

    /** Initializes the database schema using the underlying DataSource */
    private void initSchema() {
        var dataSource = beanScope.get(DataSource.class);
        try (var connection = dataSource.getConnection()) {
            new DatabaseInitializer().createTables(connection);
            LOGGER.info("Database schema initialized successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize database schema", e);
            throw new RuntimeException(e);
        }
    }

    /** Registers a filter obtained from the DI scope */
    private void registerFilter(ServletContext ctx, Class<? extends Filter> filterClass) {
        var filterInstance = beanScope.get(filterClass);
        var registration = ctx.addFilter(filterClass.getSimpleName(), filterInstance);

        if (registration != null) {
            registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
            LOGGER.info("Registered filter: {}", filterClass.getSimpleName());
        }
    }

    /** Registers a servlet and maps it automatically */
    private void registerServlet(ServletContext ctx, Servlet servlet) {
        var servletName = servlet.getClass().getSimpleName();
        var registration = ctx.addServlet(servletName, servlet);

        if (registration != null) {
            var mapping = getMapping(servlet);
            var finalMapping = mapping.startsWith("/") ? mapping : "/" + mapping;
            registration.addMapping(finalMapping);
            registration.setLoadOnStartup(1);

            LOGGER.info("Registered servlet: {} -> {}", servletName, finalMapping);
        }
    }

    /** Determines the servlet mapping path.
     * Reads the custom @WebRoute annotation.
     */
    private String getMapping(Servlet servlet, String... urlFragments) {
        if (urlFragments.length > 0) {
            return String.join("/", urlFragments);
        }

        var annotation = servlet.getClass().getAnnotation(WebRoute.class);
        var result = annotation != null ? annotation.value() : servlet.getClass().getSimpleName();

        return result;
    }

    /** Loads the logging configuration from the classpath */
    private void loadLoggingConfiguration() {
        var filename = "/logging.properties";
        try (var is = getClass().getResourceAsStream(filename)) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
            }
        } catch (Exception ex) {
            System.err.println("Failed to load '" + filename + "': " + ex.getMessage());
        }
    }

    /**
     * Returns the global BeanScope.
     * @return Initialized BeanScope
     */
    public static BeanScope getBeanScope() {
        if (beanScope == null) {
            throw new IllegalStateException("BeanScope is not initialized yet.");
        }
        return beanScope;
    }
}