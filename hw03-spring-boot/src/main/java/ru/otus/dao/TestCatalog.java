package ru.otus.dao;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.otus.domain.AttestationTest;
import ru.otus.domain.Question;
import ru.otus.exceptions.QuestionReadException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TestCatalog {

    private static final String CATALOG = "tests/catalog.csv";

    private static final int HEADER_PARTS = 2;

    private final QuestionCsvReader reader;

    private final List<AttestationTest> tests;

    public TestCatalog(QuestionCsvReader reader) {
        this.reader = reader;
        this.tests = List.copyOf(readCatalog());
    }

    public List<AttestationTest> findAll() {
        return tests;
    }

    public Optional<AttestationTest> find(String code) {
        return tests.stream().filter(test -> test.code().equals(code)).findFirst();
    }

    public List<Question> questions(AttestationTest test) {
        return reader.read(test.resource());
    }

    private List<AttestationTest> readCatalog() {
        List<AttestationTest> loaded = new ArrayList<>();
        for (var line : readLines(CATALOG)) {
            collect(loaded, line);
        }
        if (loaded.isEmpty()) {
            throw new QuestionReadException("Каталог тестов пуст", new IllegalStateException(CATALOG));
        }
        return loaded;
    }

    private void collect(List<AttestationTest> loaded, String line) {
        if (isSkippable(line)) {
            return;
        }
        var test = parseLine(line);
        reader.read(test.resource());
        loaded.add(test);
    }

    private boolean isSkippable(String line) {
        var value = line.trim();
        return value.isEmpty() || value.startsWith("code;");
    }

    private AttestationTest parseLine(String line) {
        var parts = line.split(";", HEADER_PARTS);
        if (parts.length < HEADER_PARTS) {
            throw broken("Некорректная строка каталога", line);
        }
        var header = headerOf(parts[1].trim());
        return new AttestationTest(parts[0].trim(), header[1], header[0], parts[1].trim());
    }

    private String[] headerOf(String resource) {
        var lines = readLines(resource);
        if (lines.isEmpty()) {
            throw broken("Пустой файл теста", resource);
        }
        var header = stripBom(lines.get(0)).split(";", HEADER_PARTS);
        if (header.length < HEADER_PARTS || header[0].isBlank() || header[1].isBlank()) {
            throw broken("В файле теста нужны преподаватель и название", resource);
        }
        return new String[] {header[0].trim(), header[1].trim()};
    }

    private List<String> readLines(String resource) {
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(resource).getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException ex) {
            throw new QuestionReadException("Не удалось прочитать " + resource, ex);
        }
    }

    private String stripBom(String line) {
        if (line.startsWith("\uFEFF")) {
            return line.substring(1);
        }
        return line;
    }

    private QuestionReadException broken(String message, String detail) {
        return new QuestionReadException(message, new IllegalArgumentException(detail));
    }
}
