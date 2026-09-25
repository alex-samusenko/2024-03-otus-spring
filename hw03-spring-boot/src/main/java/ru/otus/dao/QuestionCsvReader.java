package ru.otus.dao;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.stereotype.Component;
import ru.otus.dao.dto.QuestionDto;
import ru.otus.domain.Question;
import ru.otus.exceptions.QuestionReadException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class QuestionCsvReader {

    public List<Question> read(String resourceName) {
        var stream = getClass().getClassLoader().getResourceAsStream(resourceName);
        if (stream == null) {
            throw new QuestionReadException("Файл вопросов не найден", new IllegalArgumentException(resourceName));
        }
        return readQuestionsFromCsv(stream);
    }

    private List<Question> readQuestionsFromCsv(InputStream inputStream) {
        try (var streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             var bufferedReader = new BufferedReader(streamReader)) {
            CsvToBean<QuestionDto> csvToBean = buildCsvToBean(bufferedReader);
            List<QuestionDto> questionDtos = csvToBean.parse();
            if (questionDtos.isEmpty()) {
                throw new QuestionReadException("No questions were found!", new IllegalStateException());
            }
            return questionDtos.stream()
                    .map(QuestionDto::toDomainObject)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new QuestionReadException("Error while parsing CSV file", ex);
        }
    }

    private CsvToBean<QuestionDto> buildCsvToBean(BufferedReader bufferedReader) {
        return new CsvToBeanBuilder<QuestionDto>(bufferedReader)
                .withSkipLines(1)
                .withSeparator(';')
                .withType(QuestionDto.class)
                .build();
    }
}
