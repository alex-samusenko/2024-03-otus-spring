package ru.otus.web.dto;

public record MentorTestView(String code, String title, int passedCount, int failedCount) {
}
