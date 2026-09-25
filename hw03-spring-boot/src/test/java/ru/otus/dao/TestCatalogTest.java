package ru.otus.dao;

import org.junit.jupiter.api.Test;
import ru.otus.domain.ChoiceType;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCatalogTest {

    @Test
    void readsMentorFromTheFirstLineOfEachTestFile() {
        var catalog = new TestCatalog(new QuestionCsvReader());
        var javaTest = catalog.find("java").orElseThrow();

        assertThat(javaTest.mentor()).isEqualTo("mentor");
        assertThat(javaTest.title()).isEqualTo("Основы Java");
        assertThat(catalog.questions(javaTest)).hasSize(5);
        assertThat(catalog.questions(javaTest)).anyMatch(question -> question.choiceType() == ChoiceType.MULTIPLE);
        assertThat(catalog.find("galaxy").orElseThrow().mentor()).isEqualTo("petrov");
        assertThat(catalog.findAll()).extracting(test -> test.mentor()).contains("mentor", "petrov");
    }
}
