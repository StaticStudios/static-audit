package net.staticstudios.audit;

import java.util.Objects;
import java.util.UUID;

public class SimpleAuditUser implements AuditUser {

    private final String id;

    private SimpleAuditUser(String id) {
        this.id = id;
    }

    public static SimpleAuditUser of(String id) {
        return new SimpleAuditUser(id);
    }

    public static SimpleAuditUser of(UUID uuid) {
        return new SimpleAuditUser(uuid.toString());
    }

    @Override
    public String getAuditId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SimpleAuditUser auditUser)) return false;
        return Objects.equals(id, auditUser.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
