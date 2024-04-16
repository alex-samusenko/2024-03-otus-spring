package ru.otus.dao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.otus.config.AppProperties;
import ru.otus.config.TestFileNameProvider;
import ru.otus.domain.Question;
import java.io.InputStream;
import java.util.List;

public class CsvQuestionDaoTest {
    private final TestFileNameProvider fileNameProviderMock = Mockito.mock(AppProperties.class);
    private QuestionDao questionDao;

    @BeforeEach
    void setup() {
        questionDao = new CsvQuestionDao(fileNameProviderMock);
    }

    @DisplayName("Resource file reading")
    @Test
    void findAllTest(){
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("questions.csv");
        Assertions.assertNotNull(inputStream);
        Mockito.when(fileNameProviderMock.getTestFileName()).thenReturn("questions.csv");
        List<Question> all = questionDao.findAll();
        Assertions.assertEquals(4, all.size(), "");
    }

}
