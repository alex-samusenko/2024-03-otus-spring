package ru.otus.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.web.dto.AttemptResultView;
import ru.otus.web.dto.AttemptSession;
import ru.otus.web.dto.SaveAnswerRequest;
import ru.otus.web.dto.StartAttemptRequest;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @PostMapping
    public AttemptSession start(@RequestBody StartAttemptRequest request) {
        return attemptService.start(request.firstName(), request.lastName());
    }

    @PutMapping("/{id}/answers")
    public void saveAnswer(@PathVariable("id") String id, @RequestBody SaveAnswerRequest request) {
        attemptService.saveAnswer(id, request.questionIndex(), request.selected());
    }

    @PostMapping("/{id}/finish")
    public AttemptResultView finish(@PathVariable("id") String id) {
        return attemptService.finish(id);
    }
}
