package ru.otus.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.web.dto.AttemptReportView;
import ru.otus.web.dto.ListenerView;
import ru.otus.web.dto.MentorTestView;
import ru.otus.web.dto.TestOptionView;

import java.util.List;

@RestController
@Profile("!console")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/api/reports/attempts")
    public List<AttemptReportView> attempts() {
        return reportService.allAttempts();
    }

    @GetMapping("/api/my/attempts")
    public List<AttemptReportView> myAttempts() {
        return reportService.myAttempts();
    }

    @GetMapping("/api/mentor/tests")
    public List<MentorTestView> mentorTests() {
        return reportService.mentorTests();
    }

    @GetMapping("/api/listeners")
    public List<ListenerView> listeners() {
        return reportService.listeners();
    }

    @GetMapping("/api/tests")
    public List<TestOptionView> tests() {
        return reportService.tests();
    }
}
