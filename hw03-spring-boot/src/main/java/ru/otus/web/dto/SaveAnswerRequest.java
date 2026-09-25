package ru.otus.web.dto;

import java.util.List;

public record SaveAnswerRequest(int questionIndex, List<Integer> selected) {
}
