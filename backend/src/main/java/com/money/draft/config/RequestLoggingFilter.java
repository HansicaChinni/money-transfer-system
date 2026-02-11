
package com.money.draft.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC);

    private final String instanceId;

    public RequestLoggingFilter() {
        String host = "unknown";
        try { host = InetAddress.getLocalHost().getHostName(); } catch (Exception ignored) {}
        this.instanceId = "draft@" + host;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        MDC.put("instanceId", instanceId);

        long start = System.nanoTime();
        Instant startInstant = Instant.now();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000L;
            Instant endInstant = Instant.now();

            org.slf4j.LoggerFactory.getLogger("http.access").info(
                    "instance={} requestId={} method={} uri={} query={} status={} start={} end={} durationMs={}",
                    instanceId, requestId, request.getMethod(), request.getRequestURI(), request.getQueryString(),
                    response.getStatus(), ISO.format(startInstant), ISO.format(endInstant), durationMs
            );
            MDC.clear();
        }
    }
}
