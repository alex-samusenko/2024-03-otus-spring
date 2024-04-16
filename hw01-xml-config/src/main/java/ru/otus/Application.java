package ru.otus;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.otus.service.TestRunnerService;

public class Application {
    public static void main(String[] args) {
        var context = new ClassPathXmlApplicationContext("/spring-context.xml");

        var testRunnerService = context.getBean(TestRunnerService.class);
        testRunnerService.run();

    }
}