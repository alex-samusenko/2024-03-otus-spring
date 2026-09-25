package ru.otus.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.otus.config.TestFileNameProvider;
import ru.otus.domain.Question;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CsvQuestionDao implements QuestionDao {

    private final TestFileNameProvider fileNameProvider;

    private final QuestionCsvReader questionCsvReader;

    @Override
    public List<Question> findAll() {
        return questionCsvReader.read(fileNameProvider.getTestFileName());
    }
}
