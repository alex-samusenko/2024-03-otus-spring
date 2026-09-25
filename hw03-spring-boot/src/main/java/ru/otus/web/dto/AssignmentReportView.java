package ru.otus.web.dto;

import java.time.Instant;

public record AssignmentReportView(
        String id,
        String listenerName,
        String testTitle,
        Instant assignedAt,
        boolean finished) {
}
