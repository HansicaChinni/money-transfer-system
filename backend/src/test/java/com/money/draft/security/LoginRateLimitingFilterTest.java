
package com.money.draft.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitingFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private ServletOutputStream servletOutputStream;

    private LoginRateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitingFilter();
        ReflectionTestUtils.setField(filter, "attempts", new ConcurrentHashMap<String, Object>());
        // Configure ObjectMapper with JSR310 support for Instant serialization
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(filter, "om", om);
    }

    @Test
    void doFilter_ShouldPassThrough_WhenNotLoginEndpoint() throws Exception {
        when(request.getRequestURI()).thenReturn("/me/balance");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldPassThrough_WhenLoginGetRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("GET");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldBlock_WhenRateLimitExceeded() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getOutputStream()).thenReturn(servletOutputStream);

        for (int i = 0; i < 4; i++) {
            filter.doFilter(request, response, chain);
        }

        filter.doFilter(request, response, chain);
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
    }

    @Test
    void doFilter_ShouldReset_AfterWindowExpires() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        for (int i = 0; i < 4; i++) {
            filter.doFilter(request, response, chain);
        }

        ConcurrentHashMap<String, Object> attempts = (ConcurrentHashMap<String, Object>)
                ReflectionTestUtils.getField(filter, "attempts");
        Object attempt = attempts.get("192.168.1.1");
        ReflectionTestUtils.setField(attempt, "windowStart", System.currentTimeMillis() - 120_000);

        filter.doFilter(request, response, chain);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_ShouldHandleDifferentIpsIndependently() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");

        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        for (int i = 0; i < 4; i++) {
            filter.doFilter(request, response, chain);
        }

        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        for (int i = 0; i < 4; i++) {
            filter.doFilter(request, response, chain);
        }

        verify(response, never()).setStatus(429);
    }
}
