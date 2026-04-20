package org.ujorm.petstore.utilities;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import jakarta.inject.Singleton;

import java.sql.Connection;
import java.util.function.Supplier;
import javax.sql.DataSource;

/** Database configuration for Avaje */
@Factory
public class JdbcConfig {

    /** Creates and configures the DataSource */
    @Bean
    @Singleton
    public DataSource dataSource() {
        var config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:petstore;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        return new HikariDataSource(config);
    }

    /** Provides a supplier of the current transaction-aware connection */
    @Bean
    @Singleton
    public Supplier<Connection> connectionSupplier(TransactionManager transactionManager) {
        return transactionManager::getConnection;
    }
}
