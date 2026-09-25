package ru.otus.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.web.dto.AssignmentReportView;
import ru.otus.web.dto.AssignmentRequest;
import ru.otus.web.dto.AssignmentView;
import ru.otus.web.dto.OpenAssignmentView;

import java.util.List;

@RestController
@Profile("!console")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/api/assignments")
    public AssignmentView create(@RequestBody AssignmentRequest request) {
        return assignmentService.create(request.username(), request.testCode());
    }

    @GetMapping("/api/assignments")
    public List<AssignmentReportView> all() {
        return assignmentService.all();
    }

    @GetMapping("/api/my/assignments")
    public List<OpenAssignmentView> mine() {
        return assignmentService.mine();
    }
}
