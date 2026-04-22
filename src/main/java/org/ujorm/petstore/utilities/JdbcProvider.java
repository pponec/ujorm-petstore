package org.ujorm.petstore.utilities;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.util.function.Supplier;
import javax.sql.DataSource;

/** Database configuration for Avaje using PostgreSQL and environment variables */
@Factory
public class JdbcProvider {

    /** Creates and configures the DataSource */
    @Bean
    @Singleton
    public DataSource dataSource() {
        var db = loadConfig();
        var config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://%s:%s/%s".formatted(
                db.host(),
                db.port(),
                db.name()));
        config.setUsername(db.user());
        config.setPassword(db.password());
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        return new HikariDataSource(config);
    }

    /** Provides a supplier of the current transaction-aware connection */
    @Bean
    @Singleton
    public Supplier<Connection> connectionSupplier(TransactionManager transactionManager) {
        return transactionManager::getConnection;
    }

    /** Database configuration data */
    public record DbConfig(
            String host,
            String port,
            String name,
            String user,
            String password
    ) {}

    /** Load database configuration from environment variables */
    public static DbConfig loadConfig() {
        return new DbConfig(
                System.getenv().getOrDefault("APP_DB_HOST", "localhost"),
                System.getenv().getOrDefault("APP_DB_PORT", "5432"),
                System.getenv().getOrDefault("APP_DB_NAME", "demo"),
                System.getenv("APP_DB_USER"),
                System.getenv("APP_DB_PASSWORD")
        );
    }
}