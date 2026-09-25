package ru.otus.dao.dto;

import com.opencsv.bean.CsvBindAndSplitByPosition;
import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;
import ru.otus.domain.Answer;
import ru.otus.domain.ChoiceType;
import ru.otus.domain.Difficulty;
import ru.otus.domain.Question;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionDto {

    @CsvBindByPosition(position = 0)
    private String text;

    @CsvBindByPosition(position = 1)
    private String difficulty;

    @CsvBindByPosition(position = 2)
    private String choiceType;

    @CsvBindAndSplitByPosition(position = 3, collectionType = ArrayList.class,
            elementType = Answer.class, converter = AnswerCsvConverter.class, splitOn = "\\|")
    private List<Answer> answers;

    public Question toDomainObject() {
        var type = ChoiceType.valueOf(choiceType);
        validateCorrectAnswers(type);
        return new Question(text, answers, Difficulty.valueOf(difficulty), type);
    }

    private void validateCorrectAnswers(ChoiceType type) {
        var correctCount = answers.stream().filter(Answer::isCorrect).count();
        if (type == ChoiceType.SINGLE && correctCount != 1) {
            throw new IllegalArgumentException("Single-choice question must have one correct answer");
        }
        if (type == ChoiceType.MULTIPLE && correctCount < 2) {
            throw new IllegalArgumentException("Multiple-choice question must have several correct answers");
        }
    }
}
