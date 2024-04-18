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
        try (InputStream inputStream = getInputStream()) {
            return readQuestionsFromCsv(inputStream);
        } catch (IOException e) {
            throw new QuestionReadException("Error reading question file", e);
        }
    }

    private InputStream getInputStream() {
        ClassLoader classLoader = getClass().getClassLoader();
        return Objects.requireNonNull(classLoader.getResourceAsStream(fileNameProvider.getTestFileName()));
    }

    private List<Question> readQuestionsFromCsv(InputStream inputStream) {

        try (InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(streamReader)) {

            CsvToBean<QuestionDto> csvToBean = buildCsvToBean(bufferedReader);
            List<QuestionDto> questionDtos = csvToBean != null ? csvToBean.parse() : null;

            if (questionDtos != null && questionDtos.isEmpty()) {
                throw new QuestionReadException("No questions were found!", new RuntimeException());
            }

            return questionDtos != null ? questionDtos.stream()
                    .map(QuestionDto::toDomainObject)
                    .collect(Collectors.toList()) : null;

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
