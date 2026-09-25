package ru.otus.web.dto;

import java.util.List;

public record QuestionView(
        int index,
        String text,
        String difficulty,
        int weight,
        String choiceType,
        List<String> options) {
}
