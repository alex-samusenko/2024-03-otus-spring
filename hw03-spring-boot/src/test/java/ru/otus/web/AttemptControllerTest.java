package ru.otus.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AttemptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanAttempts() {
        jdbc.update("DELETE FROM attestation_attempt");
        jdbc.update("DELETE FROM test_assignment");
    }

    @Test
    void listenerPassesAssignedTestWhenEveryAnswerMatches() throws Exception {
        var listener = login("user");
        var attemptId = startJava(listener);
        answerJava(listener, attemptId);
        mockMvc.perform(post("/api/attempts/" + attemptId + "/finish").session(listener))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scorePercent").value(100))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.mistakes").isEmpty());
        mockMvc.perform(get("/api/reports/attempts").session(login("manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].testTitle").value("Основы Java"))
                .andExpect(jsonPath("$[0].passed").value(true))
                .andExpect(jsonPath("$[0].scorePercent").value(100))
                .andExpect(jsonPath("$[0].listenerName").value("Демо Слушатель"));
    }

    @Test
    void anonymousCannotStart() throws Exception {
        mockMvc.perform(post("/api/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentId\":\"missing\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String startJava(MockHttpSession listener) throws Exception {
        var created = mockMvc.perform(post("/api/assignments").session(login("manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"testCode\":\"java\"}"))
                .andExpect(status().isOk())
                .andReturn();
        var assignmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id").toString();
        var start = mockMvc.perform(post("/api/attempts").session(listener)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentId\":\"" + assignmentId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(5))
                .andExpect(jsonPath("$.questions[0].correct").doesNotExist())
                .andExpect(jsonPath("$.questions[1].choiceType").value("MULTIPLE"))
                .andReturn();
        return JsonPath.read(start.getResponse().getContentAsString(), "$.id").toString();
    }

    private void answerJava(MockHttpSession listener, String attemptId) throws Exception {
        save(listener, attemptId, 0, "[1]");
        save(listener, attemptId, 1, "[1,2]");
        save(listener, attemptId, 2, "[3]");
        save(listener, attemptId, 3, "[1]");
        save(listener, attemptId, 4, "[4]");
    }

    private MockHttpSession login(String username) throws Exception {
        var result = mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    private void save(MockHttpSession session, String id, int index, String selected) throws Exception {
        mockMvc.perform(put("/api/attempts/" + id + "/answers").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIndex\":" + index + ",\"selected\":" + selected + "}"))
                .andExpect(status().isOk());
    }
}
