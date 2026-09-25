package ru.otus.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.dao.QuestionDao;
import ru.otus.domain.Answer;
import ru.otus.domain.ChoiceType;
import ru.otus.domain.Difficulty;
import ru.otus.domain.Question;
import ru.otus.domain.Student;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestServiceImplTest {

    @Mock
    private IOService ioService;

    @Mock
    private LocalizedMessagesService messages;

    @Mock
    private QuestionDao questionDao;

    @Spy
    private AnswerKeys answerKeys = new AnswerKeys();

    @InjectMocks
    private TestServiceImpl testService;

    @Test
    void countsTheRightAnswer() {
        var question = new Question("Q", List.of(new Answer("yes", true), new Answer("no", false)),
                Difficulty.EASY, ChoiceType.SINGLE);
        when(questionDao.findAll()).thenReturn(List.of(question));
        when(messages.getMessage(anyString())).thenReturn("prompt");
        when(ioService.readIntForRangeWithPrompt(anyInt(), anyInt(), anyString(), anyString())).thenReturn(1);

        var result = testService.executeTestFor(new Student("Ivan", "Petrov"));

        assertThat(result.getRightAnswersCount()).isEqualTo(1);
        assertThat(result.getScore()).isEqualTo(10);
        assertThat(result.getScorePercent()).isEqualTo(100);
        assertThat(result.getAnsweredQuestions()).containsExactly(question);
    }

    @Test
    void multipleChoiceScoresOnlyTheFullSet() {
        var question = new Question("Q", List.of(
                new Answer("a", true), new Answer("b", true), new Answer("c", false)),
                Difficulty.HARD, ChoiceType.MULTIPLE);
        when(questionDao.findAll()).thenReturn(List.of(question));
        when(messages.getMessage(anyString())).thenReturn("prompt");
        when(ioService.readIntSetForRangeWithPrompt(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Set.of(1, 2));

        var result = testService.executeTestFor(new Student("Ivan", "Petrov"));

        assertThat(result.getScore()).isEqualTo(40);
        assertThat(result.getRightAnswersCount()).isEqualTo(1);
    }
}
