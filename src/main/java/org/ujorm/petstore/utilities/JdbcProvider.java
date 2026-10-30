package org.ujorm.petstore.utilities;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.jdbcx.JdbcDataSource;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import jakarta.inject.Singleton;

import java.sql.Connection;
import java.util.function.Supplier;
import javax.sql.DataSource;

/** Database configuration for Avaje */
@Factory
public class JdbcProvider {

    /**
     * Creates and configures the pooled DataSource.
     * <p>
     * The pool is given a ready H2 {@link JdbcDataSource} instead of a JDBC URL on purpose.
     * A URL makes HikariCP resolve the driver through {@code DriverManager}, whose registry is
     * frozen while a GraalVM native image is being built, so the lookup returns nothing at
     * runtime. Wiring the driver statically works on the JVM and in the native image alike.
     */
    @Bean
    @Singleton
    public DataSource dataSource() {
        var h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:petstore;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        h2.setUser("sa");
        h2.setPassword(""); // Change it

        var config = new HikariConfig();
        config.setDataSource(h2);
        return new HikariDataSource(config);
    }

    /** Provides a supplier of the current transaction-aware connection */
    @Bean
    @Singleton
    public Supplier<Connection> connectionSupplier(TransactionManager transactionManager) {
        return transactionManager::getConnection;
    }
}
