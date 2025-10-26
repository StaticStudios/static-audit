package net.staticstudios.audit;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class BasicTest extends AuditTest {
    private StaticAudit audit;

    @BeforeEach
    public void setUp() {
        audit = StaticAudit.builder()
                .applicationGroup("test")
                .applicationId("test-app")
                .connectionSupplier(AuditTest::getConnection)
                .async(false)
                .closeConnections(false)
                .build();
    }

    @Test
    public void testTableCreation() throws SQLException {
        Connection connection = getConnection();
        @Language("SQL") String sql = "SELECT to_regclass('%s.%s')";
        ResultSet rs = connection.createStatement().executeQuery(sql.formatted(audit.getSchemaName(), audit.getTableName()));

        assertTrue(rs.next());
        assertEquals(audit.getTableName(), rs.getString(1));
    }

    @Test
    public void testActionRegistration() {
        audit.registerAction(Action.simple("action1", SimpleActionData.class));
        audit.registerAction(Action.simple("action2", SimpleActionData.class));

        assertThrows(IllegalArgumentException.class, () -> audit.registerAction(Action.simple("action1", SimpleActionData.class)));
        assertThrows(IllegalArgumentException.class, () -> audit.registerAction(Action.simple("action2", SimpleActionData.class)));
    }

    @Test
    public void testSimpleActionSerialization() {
        Action<SimpleActionData> action = Action.simple("action1", SimpleActionData.class);
        SimpleActionData data = new SimpleActionData("test");
        String json = action.toJson(data);
        assertEquals("{\"data\":\"test\"}", json);
        SimpleActionData deserialized = action.fromJson(json);
        assertEquals(data.data(), deserialized.data());
    }
}
