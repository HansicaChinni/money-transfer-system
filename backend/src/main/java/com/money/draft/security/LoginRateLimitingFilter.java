package com.money.draft.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.money.draft.dto.ErrorResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class LoginRateLimitingFilter implements Filter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;

        if ("/auth/login".equals(req.getRequestURI()) && "POST".equalsIgnoreCase(req.getMethod())) {
            String ip = req.getRemoteAddr();
            Attempt att = attempts.computeIfAbsent(ip, k -> new Attempt());

            synchronized (att) {
                if (att.isWindowExpired()) {
                    attempts.remove(ip);
                    att = attempts.computeIfAbsent(ip, k -> new Attempt());
                }
                if (att.count >= MAX_ATTEMPTS) {
                    res.setStatus(429);
                    res.setContentType("application/json");
                    ErrorResponse body = new ErrorResponse(
                            "RATE_LIMITED",
                            "Too many login attempts. Try again in 1 minute.",
                            req.getRequestURI(),
                            Instant.now()
                    );
                    om.writeValue(res.getOutputStream(), body);
                    return;
                }
                att.count++;
            }
        }

        chain.doFilter(servletRequest, servletResponse);
    }

    private static class Attempt {
        int count = 1;
        final long windowStart = System.currentTimeMillis();

        boolean isWindowExpired() {
            return System.currentTimeMillis() - windowStart > WINDOW_MS;
        }
    }
}
