package ru.otus.web.dto;

import java.time.Instant;

public record AttemptReportView(
        String testTitle,
        Instant finishedAt,
        int scorePercent,
        boolean passed,
        String listenerName) {
}
