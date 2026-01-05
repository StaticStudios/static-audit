package net.staticstudios.audit;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LoggingTest extends AuditTest {
    private static final Instant NOW = Instant.ofEpochMilli(0);
    private StaticAudit audit;
    private AuditUser auditUser;
    private UUID sessionId;
    private Action<SimpleActionData> action1;
    private Action<SimpleActionData> action2;
    private Action<SimpleActionData> action3;

    @BeforeEach
    public void setUp() {
        audit = StaticAudit.builder()
                .applicationGroup("test")
                .applicationId("test-app")
                .connectionSupplier(AuditTest::getConnection)
                .async(false)
                .closeConnections(false)
                .build();

        auditUser = AuditUser.of(UUID.randomUUID());
        sessionId = UUID.randomUUID();
        action1 = Action.simple("test_action", SimpleActionData.class);
        action2 = Action.simple("test_action_2", SimpleActionData.class);
        action3 = Action.simple("test_action_3", SimpleActionData.class);
        audit.registerAction(action1);
        audit.registerAction(action2);
        audit.registerAction(action3);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        Connection connection = getConnection();
        @Language("SQL") String sql = "DROP TABLE %s.%s";
        connection.createStatement().execute(sql.formatted(audit.getSchemaName(), audit.getTableName()));
    }

    @Test
    public void testLogging() throws SQLException {
        SimpleActionData data = new SimpleActionData("test");
        audit.log(auditUser, sessionId, action1, data);

        Connection connection = getConnection();
        @Language("SQL") String sql = "SELECT * FROM %s.%s WHERE user_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql.formatted(audit.getSchemaName(), audit.getTableName()));
        statement.setObject(1, auditUser.getId());
        ResultSet rs = statement.executeQuery();
        assertTrue(rs.next());
        assertEquals(auditUser.getId(), rs.getObject("user_id"));
        assertEquals(sessionId, rs.getObject("session_id"));
        assertEquals(action1.getActionId(), rs.getString("action_id"));
        assertEquals(data, action1.fromJson(rs.getString("action_data")));
    }

    @Test
    public void testRetrieving() {
        logMultiple(50);


        List<AuditLogEntry<?>> entries;
        entries = audit.retrieve(auditUser, null, null, null, 100);
        assertEquals(50, entries.size());
        entries = audit.retrieve(auditUser, null, null, null, 10);
        assertEquals(10, entries.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("test" + (49 - i), ((SimpleActionData) entries.get(i).getData()).data());
        }
    }

    @Test
    public void testRetrievingWithFilter() {
        logMultiple(action1, 50);
        logMultiple(action2, 30);
        logMultiple(action3, 60);

        List<AuditLogEntry<?>> entries;

        entries = audit.retrieve(auditUser, null, null, null, 500);
        assertEquals(140, entries.size());
        entries = audit.retrieve(auditUser, null, null, null, 100, action1.getActionId(), action3.getActionId());
        assertEquals(100, entries.size());
        assertFalse(entries.stream().anyMatch(entry -> entry.getAction().getActionId().equals(action2.getActionId())));
        entries = audit.retrieve(auditUser, null, null, null, 10, action1.getActionId());
        assertEquals(10, entries.size());
        assertTrue(entries.stream().allMatch(entry -> entry.getAction().getActionId().equals(action1.getActionId())));
        assertFalse(entries.stream().anyMatch(entry -> entry.getAction().getActionId().equals(action2.getActionId())));
        assertFalse(entries.stream().anyMatch(entry -> entry.getAction().getActionId().equals(action3.getActionId())));
    }

    @Test
    public void testRetrievingEncoded() {
        logMultiple(action1, 5);
        logMultiple(action2, 3);
        List<EncodedAuditLogEntry> encodedEntries = audit.retrieveEncoded(auditUser, null, null, null, 20);
        assertEquals(8, encodedEntries.size());
        assertTrue(encodedEntries.stream().anyMatch(e -> e.getActionId().equals(action1.getActionId())));
        assertTrue(encodedEntries.stream().anyMatch(e -> e.getActionId().equals(action2.getActionId())));
    }

    @Test
    public void testRetrievingEncodedWithUnknownAction() throws SQLException {
        logMultiple(action1, 2);
        String unknownActionId = "unknown_action";
        String jsonData = "{\"data\":\"foobar\"}";
        Connection connection = getConnection();
        String sql = String.format("INSERT INTO %s.%s (log_id, timestamp, session_id, application_group, application_id, user_id, action_id, action_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)", audit.getSchemaName(), audit.getTableName());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, Timestamp.from(Instant.now()));
            statement.setObject(3, sessionId);
            statement.setString(4, audit.getApplicationGroup());
            statement.setString(5, audit.getApplicationId());
            statement.setObject(6, auditUser.getId());
            statement.setString(7, unknownActionId);
            statement.setString(8, jsonData);
            statement.executeUpdate();
        }
        List<EncodedAuditLogEntry> encodedEntries = audit.retrieveEncoded(auditUser, null, null, null, 10);
        assertTrue(encodedEntries.stream().anyMatch(e -> e.getActionId().equals(unknownActionId)));
    }

    @Test
    public void testRetrievingWithUnknownActionIsFiltered() throws SQLException {
        logMultiple(action1, 2);
        String unknownActionId = "unknown_action";
        String jsonData = "{\"data\":\"foobar\"}";
        Connection connection = getConnection();
        String sql = String.format("INSERT INTO %s.%s (log_id, timestamp, session_id, application_group, application_id, user_id, action_id, action_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)", audit.getSchemaName(), audit.getTableName());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, Timestamp.from(Instant.now()));
            statement.setObject(3, sessionId);
            statement.setString(4, audit.getApplicationGroup());
            statement.setString(5, audit.getApplicationId());
            statement.setObject(6, auditUser.getId());
            statement.setString(7, unknownActionId);
            statement.setString(8, jsonData);
            statement.executeUpdate();
        }
        List<AuditLogEntry<?>> entries = audit.retrieve(auditUser, null, null, null, 10);
        assertTrue(entries.stream().noneMatch(e -> e.getAction().getActionId().equals(unknownActionId)));
    }

    private void logMultiple(int count) {
        logMultiple(action1, count);
    }

    private void logMultiple(Action action, int count) {
        for (int i = 0; i < count; i++) {
            SimpleActionData data = new SimpleActionData("test" + i);
            Instant timestamp = NOW.plusSeconds(i);
            audit.log(auditUser, sessionId, timestamp, action, data);
        }
    }
}
