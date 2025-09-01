package net.staticstudios.audit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an entry in the audit log.
 */
public class AuditLogEntry<T extends ActionData> {
    private final @NotNull String actorType;
    private final @NotNull UUID actorId;
    private final @Nullable UUID sessionId;
    private final @NotNull String applicationGroup;
    private final @NotNull String applicationId;
    private final @NotNull Instant timestamp;
    private final @NotNull Action<T> action;
    private final @NotNull T data;

    /**
     * Creates a new AuditLogEntry.
     *
     * @param actorType        the type of actor who performed the action (e.g., "user", "server")
     * @param actorId          the ID of the actor who performed the action
     * @param sessionId        the ID of the session in which the action was performed (nullable)
     * @param applicationGroup the application group that logged the action
     * @param applicationId    the application ID that logged the action
     * @param timestamp        the timestamp when the action was performed
     * @param action           the action that was performed
     * @param data             the data associated with the action
     */
    public AuditLogEntry(@NotNull String actorType, @NotNull UUID actorId, @Nullable UUID sessionId, @NotNull String applicationGroup, @NotNull String applicationId, @NotNull Instant timestamp, @NotNull Action<T> action, @NotNull T data) {
        this.actorType = actorType;
        this.actorId = actorId;
        this.sessionId = sessionId;
        this.applicationGroup = applicationGroup;
        this.applicationId = applicationId;
        this.timestamp = timestamp;
        this.action = action;
        this.data = data;
    }

    /**
     * Gets the type of actor who performed the action.
     *
     * @return the actor type (e.g., "user", "server")
     */
    public @NotNull String getActorType() {
        return actorType;
    }

    /**
     * Gets the ID of the actor who performed the action.
     *
     * @return the actor ID
     */
    public @NotNull UUID getActorId() {
        return actorId;
    }

    /**
     * Gets the session ID in which the action was performed.
     *
     * @return the session ID, or null if not available
     */
    public @Nullable UUID getSessionId() {
        return sessionId;
    }

    /**
     * Gets the application group that logged the action.
     *
     * @return the application group
     */
    public @NotNull String getApplicationGroup() {
        return applicationGroup;
    }

    /**
     * Gets the application ID that logged the action.
     *
     * @return the application ID
     */
    public @NotNull String getApplicationId() {
        return applicationId;
    }

    /**
     * Gets the timestamp when the action was performed.
     *
     * @return the timestamp
     */
    public @NotNull Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the action that was performed.
     *
     * @return the action
     */
    public @NotNull Action<T> getAction() {
        return action;
    }

    /**
     * Gets the data associated with the action.
     *
     * @return the action data
     */
    public @NotNull T getData() {
        return data;
    }

    /**
     * Returns a string representation of this AuditLogEntry.
     *
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return String.format("AuditLogEntry{actorType=%s, actorId=%s, sessionId=%s, applicationGroup='%s', applicationId='%s', timestamp=%s, action=%s, data=%s}",
                actorType, actorId, sessionId, applicationGroup, applicationId, timestamp, action, data);
    }

    /**
     * Checks if this AuditLogEntry is equal to another object.
     *
     * @param obj the object to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AuditLogEntry<?> that = (AuditLogEntry<?>) obj;
        return Objects.equals(action, that.action) && Objects.equals(data, that.data) &&
                Objects.equals(actorType, that.actorType) &&
                Objects.equals(actorId, that.actorId) && Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(applicationGroup, that.applicationGroup) && Objects.equals(applicationId, that.applicationId)
                && Objects.equals(timestamp, that.timestamp);
    }

    /**
     * Returns the hash code for this AuditLogEntry.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(action, data, actorType, actorId, sessionId, applicationGroup, applicationId, timestamp);
    }
}
