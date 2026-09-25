package ru.otus.db;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!console")
@RequiredArgsConstructor
public class AssignmentRepository {

    private static final String INSERT = """
            INSERT INTO test_assignment (id, listener_id, test_code, assigned_by, assigned_at)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String BY_ID = """
            SELECT id, listener_id, test_code
            FROM test_assignment
            WHERE id = ?
            """;

    private static final String OPEN = """
            SELECT id, listener_id, test_code
            FROM test_assignment assignment
            WHERE listener_id = ?
              AND NOT EXISTS (
                  SELECT 1 FROM attestation_attempt attempt
                  WHERE attempt.assignment_id = assignment.id AND attempt.finished_at IS NOT NULL
              )
            ORDER BY assigned_at
            """;

    private static final String ALL = """
            SELECT assignment.id, assignment.test_code, assignment.assigned_at,
                   moodle_user.firstname, moodle_user.lastname,
                   EXISTS (
                       SELECT 1 FROM attestation_attempt attempt
                       WHERE attempt.assignment_id = assignment.id AND attempt.finished_at IS NOT NULL
                   ) AS finished
            FROM test_assignment assignment
            JOIN mdl_user moodle_user ON moodle_user.id = assignment.listener_id
            ORDER BY assignment.assigned_at DESC
            """;

    private final JdbcTemplate jdbc;

    public void insert(String id, long listenerId, String testCode, long assignedBy, long assignedAt) {
        jdbc.update(INSERT, id, listenerId, testCode, assignedBy, assignedAt);
    }

    public Optional<AssignmentRow> findById(String id) {
        var found = jdbc.query(BY_ID, (rs, row) -> toRow(rs), id);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(found.get(0));
    }

    public List<AssignmentRow> findOpen(long listenerId) {
        return jdbc.query(OPEN, (rs, row) -> toRow(rs), listenerId);
    }

    public List<AssignmentListRow> listAll() {
        return jdbc.query(ALL, (rs, row) -> toListRow(rs));
    }

    private AssignmentRow toRow(ResultSet rs) throws SQLException {
        return new AssignmentRow(rs.getString("id"), rs.getLong("listener_id"), rs.getString("test_code"));
    }

    private AssignmentListRow toListRow(ResultSet rs) throws SQLException {
        var name = rs.getString("lastname") + " " + rs.getString("firstname");
        return new AssignmentListRow(rs.getString("id"), rs.getString("test_code"), rs.getLong("assigned_at"),
                name, rs.getBoolean("finished"));
    }
}
