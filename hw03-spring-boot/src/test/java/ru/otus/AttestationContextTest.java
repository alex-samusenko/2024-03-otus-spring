package ru.otus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.otus.dao.QuestionDao;
import ru.otus.service.LocalizedMessagesService;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AttestationContextTest {

    @Autowired
    private QuestionDao questionDao;

    @Autowired
    private LocalizedMessagesService messages;

    @Test
    void contextLoadsRussianAttestation() {
        assertThat(questionDao.findAll()).hasSize(5);
        assertThat(questionDao.findAll().get(0).text()).isEqualTo("Есть ли жизнь на Марсе?");
        assertThat(questionDao.findAll().stream()
                .mapToInt(question -> question.difficulty().getWeight()).sum()).isEqualTo(100);
        assertThat(messages.getMessage("result.title")).isEqualTo("Результаты аттестации:");
    }
}
