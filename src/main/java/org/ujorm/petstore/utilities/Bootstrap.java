package org.ujorm.petstore.utilities;

import io.avaje.inject.BeanScope;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/** Initializes the Avaje BeanScope and Database Schema */
@WebListener
public class Bootstrap implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);
    private static BeanScope beanScope;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Initializing Ujorm PetStore with Avaje...");
        beanScope = BeanScope.builder().build();
        initSchema();
        LOGGER.info("Initialization complete.");
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
        try (var connection = dataSource.getConnection();
             var is = getClass().getResourceAsStream("/schema.sql")) {

            if (is == null) {
                throw new IllegalStateException("schema.sql not found in classpath");
            }

            var sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (var statement : sql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    try (var stmt = connection.createStatement()) {
                        stmt.execute(statement);
                    }
                }
            }
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
