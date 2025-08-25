package net.staticstudios.audit;

import net.staticstudios.utils.ThreadUtilProvider;
import net.staticstudios.utils.ThreadUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class AuditTest {
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16.2"
    );
    private static Connection connection;

    @BeforeAll
    static void initPostgres() throws SQLException {
        postgres.start();

        connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @AfterAll
    public static void cleanup() {
        postgres.stop();
    }

    public static Connection getConnection() {
        return connection;
    }

    @BeforeEach
    public void setupEnvironment() {
        ThreadUtils.setProvider(ThreadUtilProvider.builder().build());
    }

}
