package ru.otus.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import ru.otus.auth.PasswordDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RoleAccessTest {

    private static final String MD5_OF_ONE = "c4ca4238a0b923820dcc509a6f75849b";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordDigest digest;

    @BeforeEach
    void cleanAttempts() {
        jdbc.update("DELETE FROM attestation.attestation_attempt");
        jdbc.update("DELETE FROM attestation.test_assignment");
    }

    @Test
    void storesListenerPasswordAsMd5() {
        var hash = jdbc.queryForObject(
                "SELECT password FROM public.mdl_user WHERE username = 'user'", String.class);
        assertThat(hash).isEqualTo(digest.md5("1"));
        assertThat(hash).isEqualTo(MD5_OF_ONE);
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"2\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminSeesListenersAndNotStaff() throws Exception {
        mockMvc.perform(get("/api/listeners").session(login("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'user')].lastName").value(hasItem("Демо")))
                .andExpect(jsonPath("$[?(@.username == 'employee')]").value(hasSize(1)))
                .andExpect(jsonPath("$[?(@.username == 'admin')]").value(hasSize(0)))
                .andExpect(jsonPath("$[?(@.username == 'mentor')]").value(hasSize(0)));
    }

    @Test
    void listenerCannotOpenManagerReport() throws Exception {
        mockMvc.perform(get("/api/reports/attempts").session(login("user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void mentorSeesOnlyOwnTests() throws Exception {
        mockMvc.perform(get("/api/mentor/tests").session(login("mentor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'java')].title").value(hasItem("Основы Java")))
                .andExpect(jsonPath("$[?(@.code == 'spring')].failedCount").value(hasItem(0)))
                .andExpect(jsonPath("$[?(@.code == 'galaxy')]").value(hasSize(0)));
        mockMvc.perform(get("/api/mentor/tests").session(login("petrov")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("galaxy"));
    }

    private MockHttpSession login(String username) throws Exception {
        var result = mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }
}
