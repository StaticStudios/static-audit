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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoggingTest extends AuditTest {
    private static final Instant NOW = Instant.ofEpochMilli(0);
    private StaticAudit audit;
    private UUID userId;
    private UUID sessionId;
    private Action.SimpleAction<SimpleActionData> action;

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
        action = (Action.SimpleAction<SimpleActionData>) Action.simple("test_action", SimpleActionData.class);
        audit.registerAction(action);
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
        audit.log(userId, sessionId, action, data);

        Connection connection = getConnection();
        @Language("SQL") String sql = "SELECT * FROM %s.%s WHERE user_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql.formatted(audit.getSchemaName(), audit.getTableName()));
        statement.setObject(1, userId);
        ResultSet rs = statement.executeQuery();
        assertTrue(rs.next());
        assertEquals(userId, rs.getObject("user_id"));
        assertEquals(sessionId, rs.getObject("session_id"));
        assertEquals(action.getActionId(), rs.getString("action_id"));
        assertEquals(data, action.fromJson(rs.getString("action_data")));
    }

    @Test
    public void testRetrieving() {
        logMultiple(50);

        List<AuditLogEntry<?>> entries;
        entries = audit.retrieve(userId, null, null, null, null, 100);
        assertEquals(50, entries.size());
        entries = audit.retrieve(userId, null, null, null, null, 10);
        assertEquals(10, entries.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("test" + (49 - i), ((SimpleActionData) entries.get(i).getData()).data());
        }
    }

    private void logMultiple(int count) {
        for (int i = 0; i < count; i++) {
            SimpleActionData data = new SimpleActionData("test" + i);
            Instant timestamp = NOW.plusSeconds(i);
            audit.log(userId, sessionId, timestamp, action, data);
        }
    }
}
