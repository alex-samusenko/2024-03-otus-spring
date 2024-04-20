package ru.otus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.config.TestConfig;
import ru.otus.domain.TestResult;

@Service
@RequiredArgsConstructor
public class ResultServiceImpl implements ResultService {

    private final TestConfig testConfig;

    private final IOService ioService;

    @Override
    public void showResult(TestResult testResult) {
        ioService.printLine("");
        ioService.printLine("Test results: ");
        ioService.printFormattedLine("Student: %s", testResult.getStudent().getFullName());
        ioService.printFormattedLine("Answered questions count: %d", testResult.getAnsweredQuestions().size());
        ioService.printFormattedLine("Right answers count: %d", testResult.getRightAnswersCount());

        if (testResult.getRightAnswersCount() >= testConfig.getRightAnswersCountToPass()) {
            ioService.printLine("\u001B[32m" + "Congratulations! You passed test!" + "\u001B[0m");
            return;
        }
        ioService.printLine("\u001B[31m" + "Sorry. You fail test." + "\u001B[0m");
    }
}
