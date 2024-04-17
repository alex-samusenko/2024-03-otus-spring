package ru.otus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.dao.QuestionDao;
import ru.otus.domain.Answer;
import ru.otus.domain.Student;
import ru.otus.domain.TestResult;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private final IOService ioService;

    private final QuestionDao questionDao;

    @Override
    public TestResult executeTestFor(Student student) {
        ioService.printLine("");
        ioService.printFormattedLine("Please answer the questions below%n");
        var questions = questionDao.findAll();
        var testResult = new TestResult(student);

        for (var question: questions) {
            int answerIndex = 0;
            int correctAnswer = 0;
            ioService.printLine(question.text());

            for (Answer answer : question.answers()) {
                answerIndex++;
                ioService.printFormattedLine("%d. %s", answerIndex, answer.text());
                if (answer.isCorrect()) correctAnswer = answerIndex;
            }

            var studentAnswer = ioService.readIntForRangeWithPrompt(1, answerIndex,
                    "Enter your choice: ", "Type the correct number!");

            var isAnswerValid = correctAnswer == studentAnswer;
            testResult.applyAnswer(question, isAnswerValid);
        }
        return testResult;
    }
}
