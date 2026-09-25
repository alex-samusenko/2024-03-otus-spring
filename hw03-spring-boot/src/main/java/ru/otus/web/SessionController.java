package ru.otus.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.web.dto.LoginRequest;
import ru.otus.web.dto.UserView;

@RestController
@RequestMapping("/api/session")
@Profile("!console")
@RequiredArgsConstructor
public class SessionController {

    private final AuthService authService;

    @PostMapping
    public UserView login(@RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request.username(), request.password(), http);
    }

    @GetMapping
    public UserView me() {
        return authService.currentView();
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest http) {
        authService.logout(http);
    }
}
