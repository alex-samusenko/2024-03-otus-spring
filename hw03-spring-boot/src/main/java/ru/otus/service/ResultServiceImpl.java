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

    private final LocalizedMessagesService messages;

    @Override
    public void showResult(TestResult testResult) {
        ioService.printLine("");
        ioService.printLine(messages.getMessage("result.title"));
        ioService.printLine(messages.getMessage("result.student", testResult.getStudent().getFullName()));
        ioService.printLine(messages.getMessage(
                "result.answered", testResult.getAnsweredQuestions().size()));
        ioService.printLine(messages.getMessage("result.right", testResult.getRightAnswersCount()));
        if (testResult.getRightAnswersCount() >= testConfig.getRightAnswersCountToPass()) {
            ioService.printLine(messages.getMessage("result.passed"));
            return;
        }
        ioService.printLine(messages.getMessage("result.failed"));
    }
}
