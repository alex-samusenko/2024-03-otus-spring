package ru.otus.web.dto;

public record ListenerView(
        String username,
        String firstName,
        String lastName,
        String email,
        String institution,
        String department) {
}
