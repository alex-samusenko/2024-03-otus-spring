package ru.otus.service;

import ru.otus.domain.Student;
import ru.otus.domain.TestResult;

public interface TestService {
    TestResult executeTestFor(Student student);
}
