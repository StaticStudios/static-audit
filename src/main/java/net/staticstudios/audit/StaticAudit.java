package net.staticstudios.audit;

import com.google.common.base.Preconditions;
import net.staticstudios.utils.ThreadUtils;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Provides a flexible and extensible audit logging system for applications.
 * <p>
 * StaticAudit allows you to register custom actions, log user actions with associated data,
 * and retrieve audit log entries from a database. It supports asynchronous logging, configurable
 * database schema/table, and connection management. Use the {@link #builder()} to configure and create
 * an instance of StaticAudit.
 */
public class StaticAudit {
    private static final @Language("SQL") String TABLE_DEF = """
            CREATE TABLE IF NOT EXISTS %s.%s (
                log_id UUID PRIMARY KEY,
                timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                session_id UUID,
                application_group VARCHAR(255) NOT NULL,
                application_id VARCHAR(255) NOT NULL,
                user_id UUID NOT NULL,
                action_id VARCHAR(255) NOT NULL,
                action_data JSONB
            );
            """;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String applicationGroup;
    private final String applicationId;
    private final String schemaName;
    private final String tableName;
    private final boolean async;
    private final boolean closeConnections;
    private final Supplier<Connection> connectionSupplier;
    private final Map<String, Action<?>> actions = new ConcurrentHashMap<>();

    private StaticAudit(String applicationGroup, String applicationId, String schemaName, String tableName, boolean async, boolean closeConnections, Supplier<Connection> connectionSupplier) {
        this.applicationGroup = applicationGroup;
        this.applicationId = applicationId;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.async = async;
        this.closeConnections = closeConnections;
        this.connectionSupplier = connectionSupplier;

        run(connection -> {
            logger.info("Creating audit log table... ({}.{})", schemaName, tableName);
            String sql = String.format(TABLE_DEF, schemaName, tableName);
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
                logger.trace(sql);
            }
        });
    }

    /**
     * Creates a new builder for configuring and creating a StaticAudit instance.
     *
     * @return A new Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the application group for the audit logs.
     * This specifies which group the application belongs to.
     * When aggregating logs, the group will typically be used.
     *
     * @return The application group.
     */
    public String getApplicationGroup() {
        return applicationGroup;
    }

    /**
     * Returns the application ID for the audit logs.
     * This specifies where logs are coming from.
     *
     * @return The application ID.
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Returns the schema name where the audit log table resides in the database.
     *
     * @return The schema name.
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Returns the table name where audit logs are stored.
     *
     * @return The table name.
     */
    public String getTableName() {
        return tableName;
    }

    private void run(ConnectionTask task) {
        try {
            Connection connection = connectionSupplier.get();
            task.accept(connection);

            if (closeConnections) {
                connection.close();
            }
        } catch (Exception e) {
            logger.error("Error running audit task", e);
        }
    }

    private void runAsync(ConnectionTask task) {
        async(() -> run(task));
    }

    private void async(Runnable runnable) {
        if (!async) {
            runnable.run();
            return;
        }
        ThreadUtils.submit(runnable);
    }

    /**
     * Registers an action that can be logged.
     *
     * @param action The action to register.
     * @return The StaticAudit instance.
     * @throws IllegalArgumentException if the action ID is already registered.
     */
    public StaticAudit registerAction(Action<?> action) {
        Preconditions.checkNotNull(action, "Action cannot be null");
        Preconditions.checkArgument(!actions.containsKey(action.getActionId()), "Action ID %s is already registered", action.getActionId());
        actions.put(action.getActionId(), action);
        return this;
    }


    /**
     * Logs an action to the audit log.
     *
     * @param userId     The ID of the user performing the action.
     * @param sessionId  The user's current session ID, if applicable.
     * @param timestamp  The timestamp of when the action occurred.
     * @param action     The action being performed.
     * @param actionData The data associated with the action.
     * @return The StaticAudit instance.
     */
    public <T extends ActionData> StaticAudit log(@NotNull UUID userId, @Nullable UUID sessionId, @NotNull Instant timestamp, @NotNull Action<T> action, @NotNull T actionData) {
        return log(userId, sessionId, timestamp, action.getActionId(), actionData);
    }

    /**
     * Logs an action to the audit log.
     *
     * @param userId     The ID of the user performing the action.
     * @param sessionId  The user's current session ID, if applicable.
     * @param action     The action being performed.
     * @param actionData The data associated with the action.
     * @return The StaticAudit instance.
     */
    public <T extends ActionData> StaticAudit log(@NotNull UUID userId, @Nullable UUID sessionId, @NotNull Action<T> action, @NotNull T actionData) {
        return log(userId, sessionId, Instant.now(), action.getActionId(), actionData);
    }

    /**
     * Logs an action to the audit log.
     *
     * @param userId     The ID of the user performing the action.
     * @param sessionId  The user's current session ID, if applicable.
     * @param actionId   The ID of the action being performed.
     * @param actionData The data associated with the action.
     * @return The StaticAudit instance.
     */
    public StaticAudit log(@NotNull UUID userId, @Nullable UUID sessionId, @NotNull String actionId, @NotNull Object actionData) {
        return log(userId, sessionId, Instant.now(), actionId, actionData);
    }

    /**
     * Logs an action to the audit log.
     *
     * @param userId     The ID of the user performing the action.
     * @param sessionId  The user's current session ID, if applicable.
     * @param timestamp  The timestamp of when the action occurred.
     * @param actionId   The ID of the action being performed.
     * @param actionData The data associated with the action.
     * @return The StaticAudit instance.
     */
    public StaticAudit log(@NotNull UUID userId, @Nullable UUID sessionId, @NotNull Instant timestamp, @NotNull String actionId, @NotNull Object actionData) {
        Preconditions.checkNotNull(userId, "User ID cannot be null");
        Preconditions.checkNotNull(timestamp, "Timestamp cannot be null");
        Preconditions.checkNotNull(actionId, "Action ID cannot be null");
        Preconditions.checkNotNull(actionData, "Action data cannot be null");
        Action<?> action = actions.get(actionId);
        Preconditions.checkNotNull(action, "Action ID %s is not registered", actionId);
        Preconditions.checkArgument(action.getDataType().isInstance(actionData), "Action data is not of type %s", action.getDataType().getName());
        String actionDataJson = toJson(action, actionData);
        Preconditions.checkNotNull(actionDataJson, "Action data JSON cannot be null");
        runAsync(connection -> {
            @Language("SQL") String sql = "INSERT INTO %s.%s (log_id, timestamp, session_id, application_group, application_id, user_id, action_id, action_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)";
            try (PreparedStatement statement = connection.prepareStatement(sql.formatted(schemaName, tableName))) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, Timestamp.from(timestamp));
                statement.setObject(3, sessionId);
                statement.setString(4, applicationGroup);
                statement.setString(5, applicationId);
                statement.setObject(6, userId);
                statement.setString(7, actionId);
                statement.setString(8, actionDataJson);
                logger.trace(statement.toString());
                statement.executeUpdate();
            }
        });

        return this;
    }

    /**
     * Retrieves audit log entries asynchronously for a given user and optional filters.
     *
     * @param userId    The ID of the user whose logs to retrieve.
     * @param sessionId The session ID to filter by, or null for any session.
     * @param from      The start timestamp for filtering, or null for no lower bound.
     * @param to        The end timestamp for filtering, or null for no upper bound.
     * @param actionIds The action IDs to filter by, or null for any action.
     * @param limit     The maximum number of entries to retrieve.
     * @return A CompletableFuture containing the list of matching audit log entries.
     */
    public CompletableFuture<List<AuditLogEntry<?>>> retrieveAsync(@NotNull UUID userId, @Nullable UUID sessionId, @Nullable Instant from, @Nullable Instant to, int limit, String... actionIds) {
        CompletableFuture<List<AuditLogEntry<?>>> future = new CompletableFuture<>();
        async(() -> future.complete(retrieve(userId, sessionId, from, to, limit, actionIds)));
        return future;
    }

    /**
     * Retrieves audit log entries for a given user and optional filters.
     *
     * @param userId    The ID of the user whose logs to retrieve.
     * @param sessionId The session ID to filter by, or null for any session.
     * @param from      The start timestamp for filtering, or null for no lower bound.
     * @param to        The end timestamp for filtering, or null for no upper bound.
     * @param actionIds The action IDs to filter by, or null for any action.
     * @param limit     The maximum number of entries to retrieve.
     * @return The list of matching audit log entries.
     */
    public List<AuditLogEntry<?>> retrieve(@NotNull UUID userId, @Nullable UUID sessionId, @Nullable Instant from, @Nullable Instant to, int limit, String... actionIds) {
        List<EncodedAuditLogEntry> encodedList = retrieveEncoded(userId, sessionId, from, to, limit, actionIds);
        encodedList.removeIf(encoded -> {
            if (!actions.containsKey(encoded.getActionId())) {
                logger.warn("Unknown action ID {} in audit log, skipping entry", encoded.getActionId());
                return true;
            } else {
                return false;
            }
        });

        List<AuditLogEntry<?>> entries = new ArrayList<>();
        for (EncodedAuditLogEntry encoded : encodedList) {
            entries.add(createEntry(encoded));
        }

        return entries;
    }

    /**
     * Retrieves encoded audit log entries asynchronously for a given user and optional filters.
     *
     * @param userId    The ID of the user whose logs to retrieve.
     * @param sessionId The session ID to filter by, or null for any session.
     * @param from      The start timestamp for filtering, or null for no lower bound.
     * @param to        The end timestamp for filtering, or null for no upper bound.
     * @param actionIds The action IDs to filter by, or null for any action.
     * @param limit     The maximum number of entries to retrieve.
     * @return A CompletableFuture containing the list of matching audit log entries.
     */
    public CompletableFuture<List<EncodedAuditLogEntry>> retrieveEncodedAsync(@NotNull UUID userId, @Nullable UUID sessionId, @Nullable Instant from, @Nullable Instant to, int limit, String... actionIds) {
        CompletableFuture<List<EncodedAuditLogEntry>> future = new CompletableFuture<>();
        async(() -> future.complete(retrieveEncoded(userId, sessionId, from, to, limit, actionIds)));
        return future;
    }

    /**
     * Retrieves encoded audit log entries for a given user and optional filters.
     *
     * @param userId    The ID of the user whose logs to retrieve.
     * @param sessionId The session ID to filter by, or null for any session.
     * @param from      The start timestamp for filtering, or null for no lower bound.
     * @param to        The end timestamp for filtering, or null for no upper bound.
     * @param actionIds The action IDs to filter by, or null for any action.
     * @param limit     The maximum number of entries to retrieve.
     * @return The list of matching audit log entries.
     */
    public List<EncodedAuditLogEntry> retrieveEncoded(@NotNull UUID userId, @Nullable UUID sessionId, @Nullable Instant from, @Nullable Instant to, int limit, String... actionIds) {
        Preconditions.checkNotNull(userId, "User ID cannot be null");
        Preconditions.checkArgument(limit > 0, "Limit must be greater than 0");
        List<EncodedAuditLogEntry> entries = new ArrayList<>();

        run(connection -> {
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM %s.%s WHERE user_id = ?");
            if (sessionId != null) {
                sqlBuilder.append(" AND session_id = ?");
            }
            if (from != null) {
                sqlBuilder.append(" AND timestamp >= ?");
            }
            if (to != null) {
                sqlBuilder.append(" AND timestamp <= ?");
            }
            if (actionIds.length > 0) {
                sqlBuilder.append(" AND action_id IN (?");

                if (actionIds.length > 1) {
                    sqlBuilder.repeat(", ?", actionIds.length - 1);
                }

                sqlBuilder.append(")");
            }

            sqlBuilder.append(" ORDER BY timestamp DESC LIMIT ?");
            String sql = sqlBuilder.toString().formatted(schemaName, tableName);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                statement.setObject(index++, userId);
                if (sessionId != null) {
                    statement.setObject(index++, sessionId);
                }
                if (from != null) {
                    statement.setObject(index++, Timestamp.from(from));
                }
                if (to != null) {
                    statement.setObject(index++, Timestamp.from(to));
                }
                for (String actionId : actionIds) {
                    statement.setString(index++, actionId);
                }
                statement.setInt(index, limit);
                logger.trace(statement.toString());
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    String _actionId = rs.getString("action_id");
                    EncodedAuditLogEntry entry = new EncodedAuditLogEntry(
                            (UUID) rs.getObject("user_id"),
                            (UUID) rs.getObject("session_id"),
                            rs.getString("application_group"),
                            rs.getString("application_id"),
                            rs.getTimestamp("timestamp").toInstant(),
                            _actionId,
                            rs.getString("action_data")
                    );
                    entries.add(entry);
                }
            }
        });

        return entries;
    }

    private <T extends ActionData> String toJson(Action<T> action, Object data) {
        return action.toJson(action.getDataType().cast(data));
    }

    private <T extends ActionData> T fromJson(Action<T> action, String json) {
        return action.fromJson(json);
    }

    private AuditLogEntry<?> createEntry(EncodedAuditLogEntry encoded) {
        return createEntry(
                encoded.getUserId(),
                encoded.getSessionId(),
                encoded.getApplicationGroup(),
                encoded.getApplicationId(),
                encoded.getTimestamp(),
                actions.get(encoded.getActionId()),
                encoded.getEncodedData()
        );
    }

    private <T extends ActionData> AuditLogEntry<T> createEntry(
            UUID userId, UUID sessionId, String applicationGroup, String applicationId, Instant timestamp,
            Action<T> action, String jsonData) {
        return new AuditLogEntry<>(userId, sessionId, applicationGroup, applicationId, timestamp,
                action, fromJson(action, jsonData));
    }

    /**
     * Builder for configuring and creating a {@link StaticAudit} instance.
     * <p>
     * Use this builder to set application metadata, database configuration, and connection management options.
     */
    public static class Builder {
        private String applicationGroup;
        private String applicationId;
        private String schemaName = "public";
        private String tableName = "audit_logs";
        private boolean async = true;
        private boolean closeConnections = true;
        private Supplier<Connection> connectionSupplier;

        private Builder() {
        }

        /**
         * Sets the application ID for the audit logs.
         * This specifies where logs are coming from.
         *
         * @param applicationId The application ID.
         * @return The builder instance.
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Sets the application group for the audit logs.
         * This specifies which group the application belongs to.
         * When aggregating logs, the group will typically be used.
         *
         * @param applicationGroup The application group.
         * @return The builder instance.
         */
        public Builder applicationGroup(String applicationGroup) {
            this.applicationGroup = applicationGroup;
            return this;
        }

        /**
         * Sets the schema name where the audit log table resides in the database.
         * Defaults to "public" if not set.
         *
         * @param schemaName The schema name.
         * @return The builder instance.
         */
        public Builder schemaName(String schemaName) {
            this.schemaName = schemaName;
            return this;
        }

        /**
         * Sets the table name for the audit logs.
         * Defaults to "audit_log" if not set.
         *
         * @param tableName The table name.
         * @return The builder instance.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * Sets whether audit logging operations should be performed asynchronously.
         * Defaults to true if not set.
         *
         * @param async True for asynchronous operations, false for synchronous.
         * @return The builder instance.
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Sets the connection supplier that provides database connections for audit logging.
         *
         * @param connectionSupplier A supplier that provides database connections.
         * @return The builder instance.
         */
        public Builder connectionSupplier(Supplier<Connection> connectionSupplier) {
            this.connectionSupplier = connectionSupplier;
            return this;
        }

        /**
         * Sets whether the provided connections should be closed after use.
         * Defaults to true if not set.
         *
         * @param closeConnections True to close connections after use, false to keep them open.
         * @return The builder instance.
         */
        public Builder closeConnections(boolean closeConnections) {
            this.closeConnections = closeConnections;
            return this;
        }

        /**
         * Builds and returns a configured StaticAudit instance.
         *
         * @return A new StaticAudit instance.
         * @throws IllegalStateException if required fields are not set.
         */
        public StaticAudit build() {
            Preconditions.checkNotNull(applicationGroup, "Application group cannot be null");
            Preconditions.checkNotNull(applicationId, "Application ID cannot be null");
            Preconditions.checkNotNull(connectionSupplier, "Connection supplier cannot be null");
            return new StaticAudit(applicationGroup, applicationId, schemaName, tableName, async, closeConnections, connectionSupplier);
        }
    }
}
