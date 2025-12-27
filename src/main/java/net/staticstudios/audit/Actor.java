package net.staticstudios.audit;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an actor that can perform actions in the audit log system.
 */
public interface Actor {

    /**
     * Get the type of actor, e.g., "user", "server", etc.
     *
     * @return the actor type
     */
    @NotNull
    String getActorType();

    /**
     * Get the unique identifier of the actor.
     *
     * @return the actor ID
     */
    @NotNull
    UUID getActorId();
}
