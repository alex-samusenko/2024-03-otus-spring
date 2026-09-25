package ru.otus.auth;

import jakarta.servlet.http.HttpServletRequest;

public record CurrentUser(long id, String username, String firstName, String lastName, UserRole role) {

    public static final String SESSION_KEY = "currentUser";

    public static CurrentUser from(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) {
            return null;
        }
        var value = session.getAttribute(SESSION_KEY);
        if (value instanceof CurrentUser user) {
            return user;
        }
        return null;
    }
}
