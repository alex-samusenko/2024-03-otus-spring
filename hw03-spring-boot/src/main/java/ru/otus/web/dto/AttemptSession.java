package ru.otus.web.dto;

import java.util.List;

public record AttemptSession(
        String id,
        int timeLimitSeconds,
        int passingScore,
        List<QuestionView> questions) {
}
