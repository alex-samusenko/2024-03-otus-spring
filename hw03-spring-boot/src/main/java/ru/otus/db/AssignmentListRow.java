package ru.otus.db;

public record AssignmentListRow(
        String id,
        String testCode,
        long assignedAt,
        String listenerName,
        boolean finished) {
}
