package ru.otus.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StreamsIOService implements IOService {

    private static final int MAX_ATTEMPTS = 10;

    private final PrintStream printStream;

    private final Scanner scanner;

    public StreamsIOService(@Value("#{T(System).out}") PrintStream printStream,
                            @Value("#{T(System).in}") InputStream inputStream) {
        this.printStream = printStream;
        this.scanner = new Scanner(inputStream);
    }

    @Override
    public void printLine(String s) {
        printStream.println(s);
    }

    @Override
    public void printFormattedLine(String s, Object... args) {
        printStream.printf(s + "%n", args);
    }

    @Override
    public String readString() {
        return scanner.nextLine();
    }

    @Override
    public String readStringWithPrompt(String prompt) {
        printLine(prompt);
        return scanner.nextLine();
    }

    @Override
    public int readIntForRange(int min, int max, String errorMessage) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            try {
                var stringValue = scanner.nextLine();
                int intValue = Integer.parseInt(stringValue);
                if (intValue < min || intValue > max) {
                    throw new IllegalArgumentException();
                }
                return intValue;
            } catch (IllegalArgumentException e) {
                printLine(errorMessage);
            }
        }
        throw new IllegalArgumentException("Error during reading int value");
    }

    @Override
    public int readIntForRangeWithPrompt(int min, int max, String prompt, String errorMessage) {
        printLine(prompt);
        return readIntForRange(min, max, errorMessage);
    }

    @Override
    public Set<Integer> readIntSetForRangeWithPrompt(int min, int max, String prompt, String errorMessage) {
        printLine(prompt);
        for (var i = 0; i < MAX_ATTEMPTS; i++) {
            try {
                return parseIndexSet(scanner.nextLine(), min, max);
            } catch (IllegalArgumentException e) {
                printLine(errorMessage);
            }
        }
        throw new IllegalArgumentException("Error during reading int value");
    }

    private Set<Integer> parseIndexSet(String raw, int min, int max) {
        var parts = raw.trim().split("[,\\s]+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            throw new IllegalArgumentException();
        }
        var values = Arrays.stream(parts).map(Integer::parseInt).collect(Collectors.toSet());
        if (values.size() != parts.length || outOfRange(values, min, max)) {
            throw new IllegalArgumentException();
        }
        return values;
    }

    private boolean outOfRange(Set<Integer> values, int min, int max) {
        return values.stream().anyMatch(value -> value < min || value > max);
    }
}
