package ru.otus.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.otus.auth.CurrentUserService;
import ru.otus.auth.UserRole;
import ru.otus.dao.TestCatalog;
import ru.otus.db.AssignmentListRow;
import ru.otus.db.AssignmentRepository;
import ru.otus.db.AssignmentRow;
import ru.otus.db.MoodleAccount;
import ru.otus.db.MoodleUserRepository;
import ru.otus.domain.AttestationTest;
import ru.otus.web.dto.AssignmentReportView;
import ru.otus.web.dto.AssignmentView;
import ru.otus.web.dto.OpenAssignmentView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Profile("!console")
@RequiredArgsConstructor
public class AssignmentService {

    private static final String LISTENER_ROLE = "user";

    private final CurrentUserService currentUser;

    private final MoodleUserRepository users;

    private final AssignmentRepository assignments;

    private final TestCatalog catalog;

    private final ActiveAttempts activeAttempts;

    public AssignmentView create(String username, String testCode) {
        var manager = currentUser.require(UserRole.MANAGER);
        var listener = requireListener(username);
        var test = requireTest(testCode);
        var id = UUID.randomUUID().toString();
        assignments.insert(id, listener.id(), test.code(), manager.id(), Instant.now().toEpochMilli());
        return new AssignmentView(id, test.code(), test.title(), listener.username());
    }

    public List<AssignmentReportView> all() {
        currentUser.require(UserRole.MANAGER);
        return assignments.listAll().stream().map(this::toReport).toList();
    }

    public List<OpenAssignmentView> mine() {
        var listener = currentUser.require(UserRole.USER);
        return assignments.findOpen(listener.id()).stream().map(this::toOpen).toList();
    }

    private AssignmentReportView toReport(AssignmentListRow row) {
        var test = requireTest(row.testCode());
        return new AssignmentReportView(row.id(), row.listenerName(), test.title(),
                Instant.ofEpochMilli(row.assignedAt()), row.finished());
    }

    private OpenAssignmentView toOpen(AssignmentRow row) {
        var test = requireTest(row.testCode());
        return new OpenAssignmentView(row.id(), test.code(), test.title(),
                activeAttempts.idForAssignment(row.id()));
    }

    private MoodleAccount requireListener(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите слушателя");
        }
        var account = users.findActive(username.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Слушатель не найден"));
        if (!LISTENER_ROLE.equals(account.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Назначать тест можно только слушателю");
        }
        return account;
    }

    private AttestationTest requireTest(String testCode) {
        if (testCode == null || testCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите тест");
        }
        return catalog.find(testCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тест не найден"));
    }
}
