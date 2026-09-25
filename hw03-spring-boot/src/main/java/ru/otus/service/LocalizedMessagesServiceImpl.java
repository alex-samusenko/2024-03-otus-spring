package ru.otus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import ru.otus.config.TestConfig;

@Service
@RequiredArgsConstructor
public class LocalizedMessagesServiceImpl implements LocalizedMessagesService {

    private final MessageSource messageSource;

    private final TestConfig testConfig;

    @Override
    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, testConfig.getLocale());
    }
}
