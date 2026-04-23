package org.ujorm.petstore.utilities;

import io.avaje.inject.BeanScope;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ujorm.petstore.DatabaseInitializer;

import javax.sql.DataSource;
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
        // Load logging configuration as the very first step to ensure ISO formatting
        loadLoggingConfiguration();
        LOGGER.info("Initializing Ujorm PetStore ecosystem...");
        var ctx = sce.getServletContext();

        // Initialize Avaje DI container
        beanScope = BeanScope.builder().build();

        // Initialize Database Schema
        initSchema();

        // Register Filters
        registerFilter(ctx, TransactionFilter.class);
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