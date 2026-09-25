package ru.otus.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.otus.config.TestConfig;
import ru.otus.dao.QuestionDao;
import ru.otus.domain.ChoiceType;
import ru.otus.domain.Question;
import ru.otus.domain.Student;
import ru.otus.domain.TestResult;
import ru.otus.service.AnswerKeys;
import ru.otus.web.dto.AttemptResultView;
import ru.otus.web.dto.AttemptSession;
import ru.otus.web.dto.MistakeView;
import ru.otus.web.dto.QuestionView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final QuestionDao questionDao;

    private final TestConfig testConfig;

    private final AnswerKeys answerKeys;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public AttemptSession start(String firstName, String lastName) {
        if (isBlank(firstName) || isBlank(lastName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите имя и фамилию");
        }
        var attempt = new Attempt(UUID.randomUUID().toString(),
                new Student(firstName.trim(), lastName.trim()),
                questionDao.findAll());
        attempts.put(attempt.getId(), attempt);
        return toSession(attempt);
    }

    public void saveAnswer(String id, int questionIndex, List<Integer> selected) {
        var attempt = require(id);
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
        var attempt = require(id);
        if (attempt.getResult() != null) {
            return attempt.getResult();
        }
        var graded = grade(attempt);
        attempt.complete(graded);
        return graded;
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

    private Attempt require(String id) {
        var attempt = attempts.get(id);
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
        return new AttemptSession(attempt.getId(), testConfig.getTimeLimitSeconds(),
                testConfig.getPassingScore(), views);
    }

    private QuestionView toView(Question question, int index) {
        List<String> options = question.answers().stream().map(answer -> answer.text()).toList();
        return new QuestionView(index, question.text(), question.difficulty().name(),
                question.difficulty().getWeight(), question.choiceType().name(), options);
    }

    private List<Integer> sorted(Set<Integer> values) {
        return values.stream().sorted().toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
