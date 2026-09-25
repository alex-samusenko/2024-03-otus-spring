package ru.otus.dao;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.otus.config.TestFileNameProvider;
import ru.otus.dao.dto.QuestionDto;
import ru.otus.domain.Question;
import ru.otus.exceptions.QuestionReadException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CsvQuestionDao implements QuestionDao {

    private final TestFileNameProvider fileNameProvider;

    @Override
    public List<Question> findAll() {
        return readQuestionsFromCsv(getInputStream());
    }

    private InputStream getInputStream() {
        var classLoader = getClass().getClassLoader();
        var fileName = fileNameProvider.getTestFileName();
        return Objects.requireNonNull(classLoader.getResourceAsStream(fileName));
    }

    private List<Question> readQuestionsFromCsv(InputStream inputStream) {
        try (var streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             var bufferedReader = new BufferedReader(streamReader)) {
            CsvToBean<QuestionDto> csvToBean = buildCsvToBean(bufferedReader);
            List<QuestionDto> questionDtos = csvToBean.parse();
            if (questionDtos.isEmpty()) {
                throw new QuestionReadException("No questions were found!", new RuntimeException());
            }
            return questionDtos.stream()
                    .map(QuestionDto::toDomainObject)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new QuestionReadException("Error while parsing CSV file", e);
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
