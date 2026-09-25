package ru.otus.web.dto;

import java.util.List;

public record MistakeView(int index, String text, List<Integer> selected, List<Integer> correct) {
}
