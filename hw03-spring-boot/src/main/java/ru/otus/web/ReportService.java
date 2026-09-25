package ru.otus.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import ru.otus.auth.CurrentUserService;
import ru.otus.auth.UserRole;
import ru.otus.dao.TestCatalog;
import ru.otus.db.AttemptRepository;
import ru.otus.db.AttemptStats;
import ru.otus.db.MoodleUserRepository;
import ru.otus.domain.AttestationTest;
import ru.otus.web.dto.AttemptReportView;
import ru.otus.web.dto.ListenerView;
import ru.otus.web.dto.MentorTestView;
import ru.otus.web.dto.TestOptionView;

import java.util.List;

@Service
@Profile("!console")
@RequiredArgsConstructor
public class ReportService {

    private final CurrentUserService currentUser;

    private final AttemptRepository attempts;

    private final TestCatalog catalog;

    private final MoodleUserRepository users;

    public List<AttemptReportView> allAttempts() {
        currentUser.require(UserRole.MANAGER);
        return attempts.findFinished();
    }

    public List<AttemptReportView> myAttempts() {
        var listener = currentUser.require(UserRole.USER);
        return attempts.findFinishedByListener(listener.id());
    }

    public List<MentorTestView> mentorTests() {
        var mentor = currentUser.require(UserRole.MENTOR);
        var stats = attempts.stats();
        return catalog.findAll().stream()
                .filter(test -> test.mentor().equals(mentor.username()))
                .map(test -> toMentorView(test, stats))
                .toList();
    }

    public List<ListenerView> listeners() {
        currentUser.requireAny(UserRole.ADMIN, UserRole.MANAGER);
        return users.findListeners();
    }

    public List<TestOptionView> tests() {
        currentUser.require(UserRole.MANAGER);
        return catalog.findAll().stream()
                .map(test -> new TestOptionView(test.code(), test.title(), test.mentor()))
                .toList();
    }

    private MentorTestView toMentorView(AttestationTest test, List<AttemptStats> stats) {
        var matched = stats.stream().filter(item -> item.testCode().equals(test.code())).findFirst();
        var passed = matched.map(AttemptStats::passedCount).orElse(0);
        var failed = matched.map(AttemptStats::failedCount).orElse(0);
        return new MentorTestView(test.code(), test.title(), passed, failed);
    }
}
