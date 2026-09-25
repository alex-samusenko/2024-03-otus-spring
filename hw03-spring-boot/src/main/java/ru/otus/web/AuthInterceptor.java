package ru.otus.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.otus.auth.CurrentUser;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublic(request) || CurrentUser.from(request) != null) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    private boolean isPublic(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod())
                || ("POST".equals(request.getMethod()) && "/api/session".equals(request.getRequestURI()));
    }
}
