package net.staticstudios.audit;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LoggingTest extends AuditTest {
    private static final Instant NOW = Instant.ofEpochMilli(0);
    private StaticAudit audit;
    private UUID userId;
    private UUID sessionId;
    private Action.SimpleAction<SimpleActionData> action1;
    private Action.SimpleAction<SimpleActionData> action2;
    private Action.SimpleAction<SimpleActionData> action3;

    @BeforeEach
    public void setUp() {
        audit = StaticAudit.builder()
                .applicationGroup("test")
                .applicationId("test-app")
                .connectionSupplier(AuditTest::getConnection)
                .async(false)
                .closeConnections(false)
                .build();

        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        action1 = (Action.SimpleAction<SimpleActionData>) Action.simple("test_action", SimpleActionData.class);
        action2 = (Action.SimpleAction<SimpleActionData>) Action.simple("test_action_2", SimpleActionData.class);
        action3 = (Action.SimpleAction<SimpleActionData>) Action.simple("test_action_3", SimpleActionData.class);
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
        audit.log(userId, sessionId, action1, data);

        Connection connection = getConnection();
        @Language("SQL") String sql = "SELECT * FROM %s.%s WHERE user_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql.formatted(audit.getSchemaName(), audit.getTableName()));
        statement.setObject(1, userId);
        ResultSet rs = statement.executeQuery();
        assertTrue(rs.next());
        assertEquals(userId, rs.getObject("user_id"));
        assertEquals(sessionId, rs.getObject("session_id"));
        assertEquals(action1.getActionId(), rs.getString("action_id"));
        assertEquals(data, action1.fromJson(rs.getString("action_data")));
    }

    @Test
    public void testRetrieving() {
        logMultiple(50);

        List<AuditLogEntry<?>> entries;
        entries = audit.retrieve(userId, null, null, null, 100);
        assertEquals(50, entries.size());
        entries = audit.retrieve(userId, null, null, null, 10);
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

        entries = audit.retrieve(userId, null, null, null, 500);
        assertEquals(140, entries.size());
        entries = audit.retrieve(userId, null, null, null, 100, action1.getActionId(), action3.getActionId());
        assertEquals(100, entries.size());
        assertFalse(entries.stream().anyMatch(entry -> entry.getAction().getActionId().equals(action2.getActionId())));
        entries = audit.retrieve(userId, null, null, null, 10, action1.getActionId());
        assertEquals(10, entries.size());
        assertTrue(entries.stream().allMatch(entry -> entry.getAction().getActionId().equals(action1.getActionId())));
        assertFalse(entries.stream().anyMatch(entry -> entry.getAction().getActionId().equals(action2.getActionId())));
        assertFalse(entries.stream().anyMatch(entry -> entry.getAction().getActionId().equals(action3.getActionId())));
    }

    private void logMultiple(int count) {
        logMultiple(action1, count);
    }

    private void logMultiple(Action action, int count) {
        for (int i = 0; i < count; i++) {
            SimpleActionData data = new SimpleActionData("test" + i);
            Instant timestamp = NOW.plusSeconds(i);
            audit.log(userId, sessionId, timestamp, action, data);
        }
    }
}
