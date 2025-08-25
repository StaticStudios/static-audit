package net.staticstudios.audit;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Represents a task that accepts a JDBC {@link Connection} and may throw a {@link SQLException}.
 * This is a functional interface intended for use with lambda expressions or method references.
 */
@FunctionalInterface
public interface ConnectionTask {
    /**
     * Performs this task using the provided JDBC connection.
     *
     * @param connection the JDBC connection to use
     * @throws SQLException if a database access error occurs
     */
    void accept(Connection connection) throws SQLException;
}
