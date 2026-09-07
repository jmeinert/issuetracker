package de.jmeinert.issuetracker.issue;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class IssueMigrationTest {

    private static final PostgreSQLContainer postgres =
        new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

    @BeforeAll
    static void startPostgres() {
        postgres.start();
    }

    @AfterAll
    static void stopPostgres() {
        postgres.stop();
    }

    @Test
    void v4_backfillsExistingIssuesWithLegacyReporter() throws Exception {
        flywayConfiguration()
            .target("3")
            .load()
            .migrate();

        try (
            Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
            );
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
            .load()
            .migrate();

        try (
            Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
            );
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

    private FluentConfiguration flywayConfiguration() {
        return Flyway.configure()
            .dataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
            );
    }
}
