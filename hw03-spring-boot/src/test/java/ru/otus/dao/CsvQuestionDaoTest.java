package ru.otus.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.otus.config.TestFileNameProvider;
import ru.otus.domain.Question;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CsvQuestionDaoTest {

    private TestFileNameProvider fileNameProvider;

    private QuestionDao questionDao;

    @BeforeEach
    void setup() {
        fileNameProvider = mock(TestFileNameProvider.class);
        questionDao = new CsvQuestionDao(fileNameProvider);
    }

    @Test
    void findAllReadsEnglishQuestions() {
        when(fileNameProvider.getTestFileName()).thenReturn("questions_en.csv");
        var questions = questionDao.findAll();
        assertThat(questions).hasSize(5);
        assertThat(questions.get(0).text()).isEqualTo("Is there life on Mars?");
        assertThat(questions.get(0).answers()).hasSize(3);
    }

    @Test
    void findAllReadsRussianQuestions() {
        when(fileNameProvider.getTestFileName()).thenReturn("questions_ru.csv");
        List<Question> questions = questionDao.findAll();
        assertThat(questions).hasSize(5);
        assertThat(questions.get(0).text()).isEqualTo("Есть ли жизнь на Марсе?");
    }
}
