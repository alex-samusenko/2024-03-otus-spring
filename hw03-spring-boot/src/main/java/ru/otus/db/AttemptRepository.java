package ru.otus.db;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.otus.web.Attempt;
import ru.otus.web.dto.AttemptReportView;
import ru.otus.web.dto.AttemptResultView;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Repository
@Profile("!console")
@RequiredArgsConstructor
public class AttemptRepository {

    private static final String INSERT = """
            INSERT INTO attestation_attempt (
                id, listener_id, assignment_id, test_code, test_title,
                started_at, finished_at, score_percent, passed, right_answers, questions_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String FINISHED = """
            SELECT attempt.test_title, attempt.finished_at, attempt.score_percent, attempt.passed,
                   moodle_user.firstname, moodle_user.lastname
            FROM attestation_attempt attempt
            JOIN mdl_user moodle_user ON moodle_user.id = attempt.listener_id
            WHERE attempt.finished_at IS NOT NULL
            """;

    private static final String FINISHED_ORDER = FINISHED + " ORDER BY attempt.finished_at DESC";

    private static final String FINISHED_BY_LISTENER = FINISHED
            + " AND attempt.listener_id = ? ORDER BY attempt.finished_at DESC";

    private static final String FINISHED_EXISTS = """
            SELECT COUNT(*) FROM attestation_attempt
            WHERE assignment_id = ? AND finished_at IS NOT NULL
            """;

    private static final String STATS = """
            SELECT test_code,
                   SUM(CASE WHEN passed THEN 1 ELSE 0 END) AS passed_count,
                   SUM(CASE WHEN passed THEN 0 ELSE 1 END) AS failed_count
            FROM attestation_attempt
            WHERE finished_at IS NOT NULL
            GROUP BY test_code
            """;

    private final JdbcTemplate jdbc;

    public void insert(Attempt attempt, AttemptResultView result, long finishedAt) {
        var meta = attempt.getMeta();
        jdbc.update(INSERT, attempt.getId(), meta.listenerId(), meta.assignmentId(), meta.testCode(),
                meta.testTitle(), meta.startedAt().toEpochMilli(), finishedAt, result.scorePercent(),
                result.passed(), result.rightAnswersCount(), result.questionsCount());
    }

    public boolean hasFinished(String assignmentId) {
        var count = jdbc.queryForObject(FINISHED_EXISTS, Integer.class, assignmentId);
        return count != null && count > 0;
    }

    public List<AttemptReportView> findFinished() {
        return jdbc.query(FINISHED_ORDER, (rs, row) -> toReport(rs));
    }

    public List<AttemptReportView> findFinishedByListener(long listenerId) {
        return jdbc.query(FINISHED_BY_LISTENER, (rs, row) -> toReport(rs), listenerId);
    }

    public List<AttemptStats> stats() {
        return jdbc.query(STATS, (rs, row) -> new AttemptStats(
                rs.getString("test_code"), rs.getInt("passed_count"), rs.getInt("failed_count")));
    }

    private AttemptReportView toReport(ResultSet rs) throws SQLException {
        var finished = Instant.ofEpochMilli(rs.getLong("finished_at"));
        var name = rs.getString("lastname") + " " + rs.getString("firstname");
        return new AttemptReportView(rs.getString("test_title"), finished, rs.getInt("score_percent"),
                rs.getBoolean("passed"), name);
    }
}
