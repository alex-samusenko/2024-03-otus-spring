package ru.otus.db;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.otus.auth.PasswordDigest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
@Profile("!console")
@Order(1)
public class MoodleSeed implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "1";

    private static final String COUNTRY = "RU";

    private static final long CREATED_AT = Instant.parse("2026-01-15T07:00:00Z").getEpochSecond();

    private static final int COLUMN_COUNT = 9;

    private static final int USERNAME = 0;

    private static final int ROLE = 1;

    private static final int ID_NUMBER = 2;

    private static final int FIRST_NAME = 3;

    private static final int LAST_NAME = 4;

    private static final int EMAIL = 5;

    private static final int INSTITUTION = 6;

    private static final int DEPARTMENT = 7;

    private static final int CITY = 8;

    private static final String UPDATE = """
            UPDATE public.mdl_user
            SET password = ?, role = ?, idnumber = ?, firstname = ?, lastname = ?, email = ?,
                institution = ?, department = ?, city = ?, country = ?,
                deleted = 0, suspended = 0, confirmed = 1
            WHERE mnethostid = 1 AND username = ?
            """;

    private static final String INSERT = """
            INSERT INTO public.mdl_user (
                username, password, idnumber, firstname, lastname, email,
                institution, department, city, country, timecreated, timemodified, role
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;

    private final PasswordDigest digest;

    public MoodleSeed(JdbcTemplate jdbc, PasswordDigest digest) {
        this.jdbc = jdbc;
        this.digest = digest;
    }

    @Override
    public void run(ApplicationArguments args) {
        lines().forEach(this::ensureLine);
    }

    private void ensureLine(String line) {
        if (line.isBlank()) {
            return;
        }
        var user = parse(line);
        if (update(user) == 0) {
            insert(user);
        }
    }

    private int update(SeedUser user) {
        return jdbc.update(UPDATE, digest.md5(DEMO_PASSWORD), user.role(), user.idNumber(),
                user.name().firstName(), user.name().lastName(), user.email(),
                user.org().institution(), user.org().department(), user.org().city(), COUNTRY, user.username());
    }

    private void insert(SeedUser user) {
        jdbc.update(INSERT, user.username(), digest.md5(DEMO_PASSWORD), user.idNumber(),
                user.name().firstName(), user.name().lastName(), user.email(),
                user.org().institution(), user.org().department(), user.org().city(), COUNTRY,
                CREATED_AT, CREATED_AT, user.role());
    }

    private SeedUser parse(String line) {
        var columns = line.split(";", -1);
        if (columns.length < COLUMN_COUNT) {
            throw new IllegalStateException("Некорректная строка пользователя Moodle");
        }
        return new SeedUser(columns[USERNAME].trim(), columns[ROLE].trim(), columns[ID_NUMBER].trim(),
                new SeedUser.PersonName(columns[FIRST_NAME].trim(), columns[LAST_NAME].trim()),
                columns[EMAIL].trim(),
                new SeedUser.OrgUnit(columns[INSTITUTION].trim(), columns[DEPARTMENT].trim(), columns[CITY].trim()));
    }

    private List<String> lines() {
        var resource = new ClassPathResource("db/moodle-users.csv");
        try (var reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().skip(1).toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Не удалось прочитать пользователей Moodle", ex);
        }
    }
}
