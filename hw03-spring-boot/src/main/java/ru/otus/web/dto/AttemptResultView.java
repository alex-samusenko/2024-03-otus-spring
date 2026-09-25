package ru.otus.web.dto;

import java.util.List;

public record AttemptResultView(
        int scorePercent,
        boolean passed,
        int rightAnswersCount,
        int questionsCount,
        List<MistakeView> mistakes) {
}
