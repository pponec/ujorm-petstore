package org.ujorm.petstore.utilities;

import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight transaction manager for JDBC.
 * Ensures that and all DAO operations within a transaction block use the same connection.
 */
@Singleton
public class TransactionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManager.class);
    private static final ThreadLocal<Connection> CONNECTION_HOLDER = new ThreadLocal<>();

    /** The data source */
    private final DataSource dataSource;

    /** Creates a new transaction manager */
    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Executes a functional block within a read-write transaction.
     * @param task Task to execute
     * @param <T> Result type
     * @return Task result
     */
    public <T> T run(SupplierThrowing<T> task) {
        return run(false, task);
    }

    /**
     * Executes a functional block within a transaction.
     * <p>
     *   Propagation is {@code REQUIRED}: when a transaction is already active on the
     *   current thread, the task simply joins it (no nested commit, the {@code readOnly}
     *   hint of the inner call is ignored — the outermost boundary wins).
     * </p>
     * @param readOnly Flag the JDBC connection as read-only (write operations may fail).
     * @param task Task to execute
     * @param <T> Result type
     * @return Task result
     */
    public <T> T run(boolean readOnly, SupplierThrowing<T> task) {
        var existingConnection = CONNECTION_HOLDER.get();
        if (existingConnection != null) {
            // Already in a transaction
            try {
                return task.get();
            } catch (Exception e) {
                throw new RuntimeException("Transaction failed", e);
            }
        }

        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setReadOnly(readOnly);
            CONNECTION_HOLDER.set(connection);
            try {
                T result = task.get();
                connection.commit();
                return result;
            } catch (Exception e) {
                connection.rollback();
                LOGGER.error("Transaction rolled back due to error", e);
                throw new RuntimeException("Transaction failed", e);
            } finally {
                CONNECTION_HOLDER.remove();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not manage connection", e);
        }
    }

    /**
     * Executes a functional block without a return value within a transaction.
     * @param task Task to execute
     */
    public void run(RunnableThrowing task) {
        run(() -> {
            task.run();
            return null;
        });
    }

    /**
     * Returns the connection bound to the current transaction.
     * <p>
     *   A transaction must be active — typically opened by a {@link Transactional}
     *   service method. There is intentionally no fallback to a fresh pool connection:
     *   such a connection would never be committed or closed (a leak) and would silently
     *   run outside any transaction. Annotate the calling service method with
     *   {@code @Transactional} (or wrap the call in {@link #run(SupplierThrowing)}).
     * </p>
     * @throws IllegalStateException when no transaction is active on the current thread.
     */
    public Connection getConnection() {
        var conn = CONNECTION_HOLDER.get();
        if (conn == null) {
            throw new IllegalStateException("No active transaction on the current thread. "
                    + "Annotate the service method with @Transactional.");
        }
        return conn;
    }

    /** Functional interface for tasks that may throw exceptions */
    @FunctionalInterface
    public interface SupplierThrowing<T> {
        T get() throws Exception;
    }

    /** Functional interface for tasks without return value that may throw exceptions */
    @FunctionalInterface
    public interface RunnableThrowing {
        void run() throws Exception;
    }
}
