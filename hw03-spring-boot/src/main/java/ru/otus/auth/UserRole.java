package ru.otus.auth;

import java.util.Arrays;
import java.util.Optional;

public enum UserRole {
    ADMIN("admin"),
    MANAGER("manager"),
    MENTOR("mentor"),
    USER("user");

    private final String code;

    UserRole(String roleCode) {
        this.code = roleCode;
    }

    public String code() {
        return code;
    }

    public static Optional<UserRole> find(String roleCode) {
        return Arrays.stream(values()).filter(role -> role.code.equals(roleCode)).findFirst();
    }
}
