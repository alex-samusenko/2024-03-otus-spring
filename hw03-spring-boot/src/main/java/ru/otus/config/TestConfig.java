package ru.otus.config;

import java.util.Locale;

public interface TestConfig {

    int getPassingScore();

    int getTimeLimitSeconds();

    Locale getLocale();
}
