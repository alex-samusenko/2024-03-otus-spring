package ru.otus.db;

public record MoodleAccount(
        long id,
        String username,
        String passwordHash,
        String firstName,
        String lastName,
        String role) {
}
