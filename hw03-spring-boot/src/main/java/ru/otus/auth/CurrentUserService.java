package ru.otus.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserService {

    public CurrentUser require() {
        var user = CurrentUser.from(request());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется вход");
        }
        return user;
    }

    public CurrentUser require(UserRole role) {
        var user = require();
        if (user.role() != role) {
            throw forbidden();
        }
        return user;
    }

    public CurrentUser requireAny(UserRole first, UserRole second) {
        var user = require();
        if (user.role() != first && user.role() != second) {
            throw forbidden();
        }
        return user;
    }

    private HttpServletRequest request() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Недостаточно прав");
    }
}
