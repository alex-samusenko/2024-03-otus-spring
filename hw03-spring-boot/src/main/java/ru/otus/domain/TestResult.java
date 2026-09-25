package ru.otus.domain;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class TestResult {

    private final Student student;

    private final List<Question> answeredQuestions;

    private int rightAnswersCount;

    private int score;

    private int maxScore;

    public TestResult(Student student) {
        this.student = student;
        this.answeredQuestions = new ArrayList<>();
    }

    public void applyAnswer(Question question, boolean isRightAnswer) {
        answeredQuestions.add(question);
        maxScore += question.difficulty().getWeight();
        if (isRightAnswer) {
            rightAnswersCount++;
            score += question.difficulty().getWeight();
        }
    }

    public int getScorePercent() {
        if (maxScore == 0) {
            return 0;
        }
        return score * 100 / maxScore;
    }
}
