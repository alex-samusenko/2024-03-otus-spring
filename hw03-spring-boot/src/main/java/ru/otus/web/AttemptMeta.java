package ru.otus.web;

import java.time.Instant;

public record AttemptMeta(
        long listenerId,
        String assignmentId,
        String testCode,
        String testTitle,
        Instant startedAt) {
}
