package ru.otus.service;

import lombok.RequiredArgsConstructor;
import ru.otus.dao.QuestionDao;
import ru.otus.domain.Question;
import ru.otus.domain.Answer;

@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private final IOService ioService;

    private final QuestionDao questionDao;

    @Override
    public void executeTest() {
        ioService.printLine("");
        ioService.printFormattedLine("Please answer the questions below%n");
        for (Question question : questionDao.findAll()) {
            int counter = 0;
            ioService.printLine(question.text());

            for (Answer answer : question.answers()) {
                counter++;
                ioService.printFormattedLine("%d. %s", counter, answer.text());
            }

            ioService.printLine("");
        }
    }
}
