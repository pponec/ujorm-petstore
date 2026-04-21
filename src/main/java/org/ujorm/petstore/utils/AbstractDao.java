package org.ujorm.petstore.utils;

import java.sql.Connection;
import java.util.function.Supplier;

public class AbstractDao {

    /** Provider of the transaction-aware connection */
    private final Supplier<Connection> connection;

    /** Creates a new Dao with the connection supplier */
    public AbstractDao(Supplier<Connection> connection) {
        this.connection = connection;
    }

    protected Connection connection() {
        return connection.get();
    }
}
