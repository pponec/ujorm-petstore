package org.ujorm.petstore.utilities;

import io.avaje.inject.BeanScope;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import jakarta.servlet.DispatcherType;
import org.ujorm.petstore.DatabaseInitializer;

/** Initializes the Avaje BeanScope and Database Schema */
@WebListener
public class Bootstrap implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);
    private static BeanScope beanScope;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Initializing Ujorm PetStore with Avaje...");
        beanScope = BeanScope.builder().build();
        registerFilter(TransactionFilter.class, sce);
        initSchema();
        LOGGER.info("Initialization complete.");
    }

    /** Registers a filter obtained from the DI scope */
    private void registerFilter(Class<? extends Filter> filterClass, ServletContextEvent sce) {
        var servletContext = sce.getServletContext();
        var filterInstance = beanScope.get(filterClass);
        var registration = servletContext.addFilter(filterClass.getSimpleName(), filterInstance);

        if (registration != null) {
            registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (beanScope != null) {
            beanScope.close();
        }
    }

    /** Initializes the database schema using the schema.sql file */
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

    /** Returns the global BeanScope */
    public static BeanScope getBeanScope() {
        if (beanScope == null) {
            throw new IllegalStateException("BeanScope is not initialized yet.");
        }
        return beanScope;
    }
}