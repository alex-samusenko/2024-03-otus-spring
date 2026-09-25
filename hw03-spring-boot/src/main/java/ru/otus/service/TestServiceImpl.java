package ru.otus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.dao.QuestionDao;
import ru.otus.domain.Answer;
import ru.otus.domain.ChoiceType;
import ru.otus.domain.Question;
import ru.otus.domain.Student;
import ru.otus.domain.TestResult;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private final IOService ioService;

    private final LocalizedMessagesService messages;

    private final QuestionDao questionDao;

    private final AnswerKeys answerKeys;

    @Override
    public TestResult executeTestFor(Student student) {
        ioService.printLine("");
        ioService.printLine(messages.getMessage("test.prompt"));
        var testResult = new TestResult(student);
        for (var question : questionDao.findAll()) {
            askQuestion(question, testResult);
        }
        return testResult;
    }

    private void askQuestion(Question question, TestResult testResult) {
        var answers = question.answers();
        ioService.printLine(question.text());
        printQuestionMeta(question);
        printAnswers(answers);
        var studentAnswers = readStudentAnswers(answers.size(), question.choiceType());
        testResult.applyAnswer(question, answerKeys.correctIndexes(question).equals(studentAnswers));
    }

    private void printQuestionMeta(Question question) {
        var difficulty = question.difficulty();
        ioService.printLine(messages.getMessage(
                "question.difficulty",
                messages.getMessage("difficulty." + difficulty.name()),
                difficulty.getWeight()));
        ioService.printLine(messages.getMessage(
                "question.choice",
                messages.getMessage("choice." + question.choiceType().name())));
    }

    private void printAnswers(List<Answer> answers) {
        var answerIndex = 0;
        for (var answer : answers) {
            answerIndex++;
            ioService.printFormattedLine("%d. %s", answerIndex, answer.text());
        }
    }

    private Set<Integer> readStudentAnswers(int optionsCount, ChoiceType choiceType) {
        if (choiceType == ChoiceType.SINGLE) {
            return Set.of(ioService.readIntForRangeWithPrompt(1, optionsCount,
                    messages.getMessage("test.answer.prompt"),
                    messages.getMessage("test.answer.error")));
        }
        return ioService.readIntSetForRangeWithPrompt(1, optionsCount,
                messages.getMessage("test.answer.prompt.multiple"),
                messages.getMessage("test.answer.error.multiple"));
    }

}
