package ru.otus.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.otus.auth.CurrentUser;
import ru.otus.auth.CurrentUserService;
import ru.otus.auth.UserRole;
import ru.otus.config.TestConfig;
import ru.otus.dao.TestCatalog;
import ru.otus.db.AssignmentRepository;
import ru.otus.db.AssignmentRow;
import ru.otus.db.AttemptRepository;
import ru.otus.domain.AttestationTest;
import ru.otus.domain.ChoiceType;
import ru.otus.domain.Question;
import ru.otus.domain.Student;
import ru.otus.domain.TestResult;
import ru.otus.service.AnswerKeys;
import ru.otus.web.dto.AttemptResultView;
import ru.otus.web.dto.AttemptSession;
import ru.otus.web.dto.MistakeView;
import ru.otus.web.dto.QuestionView;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Profile("!console")
@RequiredArgsConstructor
public class AttemptService {

    private final TestCatalog catalog;

    private final TestConfig testConfig;

    private final AnswerKeys answerKeys;

    private final AssignmentRepository assignments;

    private final AttemptRepository attemptRepository;

    private final ActiveAttempts activeAttempts;

    private final CurrentUserService currentUser;

    public AttemptSession start(String assignmentId) {
        var listener = currentUser.require(UserRole.USER);
        if (assignmentId == null || assignmentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не выбрано назначение");
        }
        return open(listener, assignmentId);
    }

    public AttemptSession read(String id) {
        var attempt = requireOwned(id);
        if (attempt.getResult() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Аттестация уже завершена");
        }
        return toSession(attempt);
    }

    public void saveAnswer(String id, int questionIndex, List<Integer> selected) {
        var attempt = requireOwned(id);
        if (attempt.getResult() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Аттестация уже завершена");
        }
        if (questionIndex < 0 || questionIndex >= attempt.getQuestions().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неизвестный вопрос");
        }
        var values = selected == null ? Set.<Integer>of() : Set.copyOf(selected);
        validateSelection(attempt.getQuestions().get(questionIndex), values);
        attempt.select(questionIndex, values);
    }

    public AttemptResultView finish(String id) {
        var attempt = requireOwned(id);
        if (attempt.getResult() != null) {
            return attempt.getResult();
        }
        var graded = grade(attempt);
        attemptRepository.insert(attempt, graded, Instant.now().toEpochMilli());
        attempt.complete(graded);
        activeAttempts.finish(attempt);
        return graded;
    }

    private AttemptSession open(CurrentUser listener, String assignmentId) {
        var assignment = requireOpen(assignmentId, listener.id());
        var existingId = activeAttempts.idForAssignment(assignmentId);
        if (existingId != null) {
            return toSession(require(existingId));
        }
        return remember(listener, assignment);
    }

    private AttemptSession remember(CurrentUser listener, AssignmentRow assignment) {
        var test = requireTest(assignment.testCode());
        var attempt = create(listener, assignment, test);
        activeAttempts.put(attempt);
        return toSession(attempt);
    }

    private Attempt create(CurrentUser listener, AssignmentRow assignment, AttestationTest test) {
        var meta = new AttemptMeta(listener.id(), assignment.id(), test.code(), test.title(), Instant.now());
        var student = new Student(listener.firstName(), listener.lastName());
        return new Attempt(UUID.randomUUID().toString(), student, catalog.questions(test), meta);
    }

    private AssignmentRow requireOpen(String assignmentId, long listenerId) {
        var assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Назначение не найдено"));
        if (assignment.listenerId() != listenerId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Назначение другого слушателя");
        }
        if (attemptRepository.hasFinished(assignmentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Это назначение уже выполнено");
        }
        return assignment;
    }

    private AttestationTest requireTest(String code) {
        return catalog.find(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тест не найден"));
    }

    private AttemptResultView grade(Attempt attempt) {
        var result = new TestResult(attempt.getStudent());
        List<MistakeView> mistakes = new ArrayList<>();
        var questions = attempt.getQuestions();
        for (var i = 0; i < questions.size(); i++) {
            collectAnswer(result, mistakes, questions.get(i), attempt.getSelected().get(i), i);
        }
        var percent = result.getScorePercent();
        return new AttemptResultView(percent, percent >= testConfig.getPassingScore(),
                result.getRightAnswersCount(), questions.size(), mistakes);
    }

    private void collectAnswer(TestResult result, List<MistakeView> mistakes, Question question,
                               Set<Integer> selected, int index) {
        var correct = answerKeys.correctIndexes(question);
        var right = correct.equals(selected);
        result.applyAnswer(question, right);
        if (!right) {
            mistakes.add(new MistakeView(index, question.text(), sorted(selected), sorted(correct)));
        }
    }

    private void validateSelection(Question question, Set<Integer> values) {
        var max = question.answers().size();
        if (values.stream().anyMatch(value -> value < 1 || value > max)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный номер ответа");
        }
        if (question.choiceType() == ChoiceType.SINGLE && values.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужен один вариант");
        }
    }

    private Attempt requireOwned(String id) {
        var listener = currentUser.require(UserRole.USER);
        var attempt = require(id);
        if (attempt.getMeta().listenerId() != listener.id()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Чужая попытка");
        }
        return attempt;
    }

    private Attempt require(String id) {
        var attempt = activeAttempts.find(id);
        if (attempt == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Попытка не найдена");
        }
        return attempt;
    }

    private AttemptSession toSession(Attempt attempt) {
        List<QuestionView> views = new ArrayList<>();
        var questions = attempt.getQuestions();
        for (var i = 0; i < questions.size(); i++) {
            views.add(toView(questions.get(i), i));
        }
        return new AttemptSession(attempt.getId(), attempt.getMeta().testTitle(),
                testConfig.getTimeLimitSeconds(), remainingSeconds(attempt),
                testConfig.getPassingScore(), views);
    }

    private int remainingSeconds(Attempt attempt) {
        var spent = Duration.between(attempt.getMeta().startedAt(), Instant.now()).toSeconds();
        return (int) Math.max(0, testConfig.getTimeLimitSeconds() - spent);
    }

    private QuestionView toView(Question question, int index) {
        List<String> options = question.answers().stream().map(answer -> answer.text()).toList();
        return new QuestionView(index, question.text(), question.difficulty().name(),
                question.difficulty().getWeight(), question.choiceType().name(), options);
    }

    private List<Integer> sorted(Set<Integer> values) {
        return values.stream().sorted().toList();
    }
}
