package ru.otus.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.otus.dao.CsvQuestionDao;
import ru.otus.domain.Student;

public class TestServiceImplTest {
    private final StreamsIOService ioService = Mockito.mock(StreamsIOService.class);
    private CsvQuestionDao csvQuestionDao;
    private TestServiceImpl testServiceImpl;

    @BeforeEach
    void setup() {
        csvQuestionDao = Mockito.mock(CsvQuestionDao.class);
        testServiceImpl = new TestServiceImpl(ioService, csvQuestionDao);
    }
    @DisplayName("Test service works properly")
    @Test
    void executeTestService() {
        Mockito.when(csvQuestionDao.findAll()).thenReturn(Mockito.anyList());
        Student student = new Student("Aleksei", "Samusenko");
        testServiceImpl.executeTestFor(student);
        Mockito.verify(ioService, Mockito.times(1)).printLine(Mockito.anyString());
        Mockito.verify(ioService, Mockito.times(1)).printFormattedLine(Mockito.anyString());
    }
}
