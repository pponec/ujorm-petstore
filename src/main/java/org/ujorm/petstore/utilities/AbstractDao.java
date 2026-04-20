package org.ujorm.petstore.utilities;

import java.sql.Connection;
import java.util.function.Supplier;

public abstract class AbstractDao {

    /** Provider of the transaction-aware connection */
    private final Supplier<Connection> connection;

    public AbstractDao(Supplier<Connection> connection) {
        this.connection = connection;
    }

    protected Connection connection() {
        return connection.get();
    }
}
