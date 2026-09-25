package ru.otus.db;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Profile("!console")
@Order(0)
@RequiredArgsConstructor
public class DatabaseMigrator implements ApplicationRunner {

    private static final String ASSIGNMENT_COLUMNS =
            "id, listener_id, test_code, assigned_by, assigned_at";

    private static final String ATTEMPT_COLUMNS = """
            id, listener_id, assignment_id, test_code, test_title, started_at, finished_at,
            score_percent, passed, right_answers, questions_count""";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/app-schema.sql"));
            moveLegacy(connection, "test_assignment", ASSIGNMENT_COLUMNS);
            moveLegacy(connection, "attestation_attempt", ATTEMPT_COLUMNS);
        } catch (SQLException ex) {
            throw new IllegalStateException("Не удалось подготовить схему аттестации", ex);
        }
    }

    private void moveLegacy(Connection connection, String table, String columns) throws SQLException {
        if (!existsInPublic(connection, table)) {
            return;
        }
        copy(connection, table, columns);
        dropPublic(connection, table);
    }

    private boolean existsInPublic(Connection connection, String table) throws SQLException {
        var sql = """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE lower(table_schema) = 'public' AND lower(table_name) = ?
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1) > 0;
            }
        }
    }

    private void copy(Connection connection, String table, String columns) throws SQLException {
        var sql = "INSERT INTO attestation." + table + " (" + columns + ") "
                + "SELECT " + columns + " FROM public." + table + " "
                + "WHERE id NOT IN (SELECT id FROM attestation." + table + ")";
        try (var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void dropPublic(Connection connection, String table) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("DROP TABLE public." + table);
        }
    }
}
