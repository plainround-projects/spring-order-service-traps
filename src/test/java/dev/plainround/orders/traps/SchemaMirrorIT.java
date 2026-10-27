package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plainround.orders.support.PostgresContainer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

/**
 * sql/001_schema.sql must describe the same schema that Flyway builds from the migrations: the same
 * columns, indexes and constraints. The migrations ran at startup; the script runs in a second,
 * empty database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportTestcontainers(PostgresContainer.class)
class SchemaMirrorIT {

    private static final String DESCRIBE =
            """
            select 'column ' || table_name || '.' || column_name || ' ' || data_type
                   || ' null=' || is_nullable || ' identity=' || is_identity
                   || ' length=' || coalesce(character_maximum_length::text, '-')
                   || ' default=' || coalesce(column_default, '-')
              from information_schema.columns
             where table_schema = 'public' and table_name <> 'flyway_schema_history'
            union all
            select 'index ' || indexdef from pg_indexes
             where schemaname = 'public' and tablename <> 'flyway_schema_history'
            union all
            select 'constraint ' || conrelid::regclass || ' ' || contype::text || ' ' || pg_get_constraintdef(oid)
              from pg_constraint
             where connamespace = 'public'::regnamespace and conrelid::regclass::text <> 'flyway_schema_history'
            order by 1
            """;

    @Autowired private DataSource flywayDatabase;

    @Test
    void theSchemaScriptBuildsTheSameSchemaAsTheMigrations() throws Exception {
        List<String> fromMigrations;
        try (Connection connection = flywayDatabase.getConnection()) {
            fromMigrations = describe(connection);
            connection.createStatement().execute("CREATE DATABASE schema_script");
        }

        List<String> fromScript;
        try (Connection connection = connectTo("schema_script")) {
            connection.createStatement().execute(Files.readString(Path.of("sql/001_schema.sql")));
            fromScript = describe(connection);
        }

        assertThat(fromMigrations).isNotEmpty();
        assertThat(fromScript).isEqualTo(fromMigrations);
    }

    private static Connection connectTo(String database) throws SQLException {
        String url =
                "jdbc:postgresql://"
                        + PostgresContainer.POSTGRES.getHost()
                        + ":"
                        + PostgresContainer.POSTGRES.getFirstMappedPort()
                        + "/"
                        + database;
        return DriverManager.getConnection(
                url,
                PostgresContainer.POSTGRES.getUsername(),
                PostgresContainer.POSTGRES.getPassword());
    }

    private static List<String> describe(Connection connection) throws SQLException {
        List<String> lines = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(DESCRIBE)) {
            while (rows.next()) {
                lines.add(rows.getString(1));
            }
        }
        return lines;
    }
}
