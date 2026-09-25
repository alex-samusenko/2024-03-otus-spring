package ru.otus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.dao.QuestionDao;
import ru.otus.domain.Answer;
import ru.otus.domain.Question;
import ru.otus.domain.Student;
import ru.otus.domain.TestResult;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private final IOService ioService;

    private final LocalizedMessagesService messages;

    private final QuestionDao questionDao;

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
        printDifficulty(question);
        var correctAnswer = printAnswers(answers);
        var studentAnswer = ioService.readIntForRangeWithPrompt(1, answers.size(),
                messages.getMessage("test.answer.prompt"), messages.getMessage("test.answer.error"));
        testResult.applyAnswer(question, correctAnswer == studentAnswer);
    }

    private void printDifficulty(Question question) {
        var difficulty = question.difficulty();
        ioService.printLine(messages.getMessage(
                "question.difficulty",
                messages.getMessage("difficulty." + difficulty.name()),
                difficulty.getWeight()));
    }

    private int printAnswers(List<Answer> answers) {
        var correctAnswer = 0;
        var answerIndex = 0;
        for (var answer : answers) {
            answerIndex++;
            ioService.printFormattedLine("%d. %s", answerIndex, answer.text());
            if (answer.isCorrect()) {
                correctAnswer = answerIndex;
            }
        }
        return correctAnswer;
    }
}
