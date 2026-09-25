package ru.otus.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.otus.auth.CurrentUser;
import ru.otus.auth.CurrentUserService;
import ru.otus.auth.PasswordDigest;
import ru.otus.auth.UserRole;
import ru.otus.db.MoodleAccount;
import ru.otus.db.MoodleUserRepository;
import ru.otus.web.dto.UserView;

@Service
@Profile("!console")
@RequiredArgsConstructor
public class AuthService {

    private final MoodleUserRepository users;

    private final PasswordDigest digest;

    private final CurrentUserService currentUser;

    public UserView login(String username, String password, HttpServletRequest request) {
        var account = authenticate(username, password);
        var user = toCurrent(account);
        request.getSession(true).setAttribute(CurrentUser.SESSION_KEY, user);
        return toView(user);
    }

    public UserView currentView() {
        return toView(currentUser.require());
    }

    public void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private MoodleAccount authenticate(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            throw unauthorized();
        }
        var account = users.findActive(username.trim()).orElseThrow(this::unauthorized);
        if (!digest.matches(password, account.passwordHash())) {
            throw unauthorized();
        }
        return account;
    }

    private CurrentUser toCurrent(MoodleAccount account) {
        var role = UserRole.find(account.role())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Роль не назначена"));
        return new CurrentUser(account.id(), account.username(), account.firstName(), account.lastName(), role);
    }

    private UserView toView(CurrentUser user) {
        return new UserView(user.username(), user.firstName(), user.lastName(), user.role().code());
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
