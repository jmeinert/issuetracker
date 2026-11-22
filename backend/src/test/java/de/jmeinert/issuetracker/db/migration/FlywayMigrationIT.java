package de.jmeinert.issuetracker.db.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIT {

    private final PostgreSQLContainer postgres =
        new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

    @BeforeEach
    void startPostgres() {
        postgres.start();
    }

    @AfterEach
    void stopPostgres() {
        postgres.stop();
    }

    @Test
    void v4_backfillsExistingIssuesWithLegacyReporter() throws SQLException {
        flywayConfiguration()
            .target("3")
            .load()
            .migrate();

        try (
            Connection connection = openConnection();
            Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO projects (id, name, description, created_at, updated_at)
                VALUES (1, 'Example project', 'Example description', now(), now());
            
                INSERT INTO issues (
                    id, title, description, status, priority, project_id,
                    created_at, updated_at
                )
                VALUES (
                    1, 'Legacy issue', 'Example description', 'OPEN', 'LOW', 1,
                    now(), now()
                );
            """);
        }

        flywayConfiguration()
            .target("4")
            .load()
            .migrate();

        try (
            Connection connection = openConnection();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("""
                SELECT
                    i.reporter_id,
                    i.assignee_id,
                    u.username,
                    u.email,
                    u.role,
                    u.enabled
                FROM issues i
                JOIN users u ON u.id = i.reporter_id
                WHERE i.id = 1
            """)
        ) {
            assertThat(result.next()).isTrue();
            assertThat(result.getObject("reporter_id")).isNotNull();
            assertThat(result.getObject("assignee_id")).isNull();
            assertThat(result.getString("username"))
                .isEqualTo("__system_legacy_issue_reporter__");
            assertThat(result.getString("email"))
                .isEqualTo("legacy-issue-reporter@issuetracker.invalid");
            assertThat(result.getString("role")).isEqualTo("USER");
            assertThat(result.getBoolean("enabled")).isFalse();
        }
    }

    @Test
    void v5_migratesUsernamesToDisplayNameAndRemovesUniqueConstraint() throws SQLException {
        flywayConfiguration()
            .target("4")
            .load()
            .migrate();

        try (
            Connection connection = openConnection();
            Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (id, username, email, password_hash, role, enabled, created_at)
                VALUES (1, 'testuser', 'testuser@example.com', 'PasswordHash', 'USER', true, now());
            """);
        }

        flywayConfiguration()
            .target("5")
            .load()
            .migrate();

        try (
            Connection connection = openConnection();
            Statement statement = connection.createStatement()
        ) {
            try (ResultSet result = statement.executeQuery("""
                SELECT display_name
                FROM users
                WHERE id = 1
                """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("display_name")).isEqualTo("testuser");
            }

            int insertedRows = statement.executeUpdate("""
                INSERT INTO users (id, display_name, email, password_hash, role, enabled, created_at)
                VALUES (2, 'testuser', 'testuser2@example.com', 'PasswordHash2', 'USER', true, now());
            """);

            assertThat(insertedRows).isEqualTo(1);
        }
    }

    private FluentConfiguration flywayConfiguration() {
        return Flyway.configure()
            .dataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
            );
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        );
    }
}
