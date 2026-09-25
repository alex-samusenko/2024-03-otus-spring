package ru.otus.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AttemptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listenerPassesWhenEveryAnswerMatches() throws Exception {
        var start = mockMvc.perform(post("/api/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Слушатель\",\"lastName\":\"Корпорация\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(5))
                .andExpect(jsonPath("$.questions[0].correct").doesNotExist())
                .andExpect(jsonPath("$.questions[1].choiceType").value("MULTIPLE"))
                .andExpect(jsonPath("$.timeLimitSeconds").value(600))
                .andReturn();
        var id = JsonPath.read(start.getResponse().getContentAsString(), "$.id").toString();
        save(id, 0, "[1]");
        save(id, 1, "[1,2]");
        save(id, 2, "[3]");
        save(id, 3, "[1]");
        save(id, 4, "[4]");

        mockMvc.perform(post("/api/attempts/" + id + "/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scorePercent").value(100))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.mistakes").isEmpty());
    }

    @Test
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"Корпорация\"}"))
                .andExpect(status().isBadRequest());
    }

    private void save(String id, int index, String selected) throws Exception {
        mockMvc.perform(put("/api/attempts/" + id + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIndex\":" + index + ",\"selected\":" + selected + "}"))
                .andExpect(status().isOk());
    }
}
