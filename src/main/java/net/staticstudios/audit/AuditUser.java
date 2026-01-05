package net.staticstudios.audit;

import java.util.Objects;
import java.util.UUID;

public class AuditUser {

    private final String id;

    private AuditUser(String id) {
        this.id = id;
    }

    public static AuditUser of(String id) {
        return new AuditUser(id);
    }

    public static AuditUser of(UUID uuid) {
        return new AuditUser(uuid.toString());
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuditUser auditUser)) return false;
        return Objects.equals(id, auditUser.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
