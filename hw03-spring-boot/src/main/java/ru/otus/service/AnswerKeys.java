package ru.otus.service;

import org.springframework.stereotype.Component;
import ru.otus.domain.Question;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class AnswerKeys {

    public Set<Integer> correctIndexes(Question question) {
        var answers = question.answers();
        return IntStream.range(0, answers.size())
                .filter(index -> answers.get(index).isCorrect())
                .map(index -> index + 1)
                .boxed()
                .collect(Collectors.toSet());
    }
}
