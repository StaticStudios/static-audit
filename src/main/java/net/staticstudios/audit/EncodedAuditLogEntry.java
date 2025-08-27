package net.staticstudios.audit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public class EncodedAuditLogEntry {
    private final @NotNull UUID userId;
    private final @Nullable UUID sessionId;
    private final @NotNull String applicationGroup;
    private final @NotNull String applicationId;
    private final @NotNull Instant timestamp;
    private final @NotNull String actionId;
    private final @NotNull String encodedData;

    public EncodedAuditLogEntry(@NotNull UUID userId, @Nullable UUID sessionId, @NotNull String applicationGroup, @NotNull String applicationId, @NotNull Instant timestamp, @NotNull String actionId, @NotNull String encodedData) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.applicationGroup = applicationGroup;
        this.applicationId = applicationId;
        this.timestamp = timestamp;
        this.actionId = actionId;
        this.encodedData = encodedData;
    }

    public @NotNull UUID getUserId() {
        return userId;
    }

    public @Nullable UUID getSessionId() {
        return sessionId;
    }

    public @NotNull String getApplicationGroup() {
        return applicationGroup;
    }

    public @NotNull String getApplicationId() {
        return applicationId;
    }

    public @NotNull Instant getTimestamp() {
        return timestamp;
    }

    public @NotNull String getActionId() {
        return actionId;
    }

    public @NotNull String getEncodedData() {
        return encodedData;
    }


}
