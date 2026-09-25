package ru.otus.service;

public interface LocalizedMessagesService {

    String getMessage(String code, Object... args);
}
