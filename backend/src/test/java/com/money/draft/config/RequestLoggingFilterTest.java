
package com.money.draft.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private RequestLoggingFilter filter;

    @BeforeEach
    void setUp() {
        MDC.clear();
        filter = new RequestLoggingFilter();
    }

    @Test
    void doFilterInternal_ShouldSetRequestId_WhenHeaderPresent() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("req-123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        when(response.getStatus()).thenReturn(200);

        FilterChain capturingChain = (req, res) -> {
            assertEquals("req-123", MDC.get("requestId"));
            assertNotNull(MDC.get("instanceId"));
        };

        filter.doFilterInternal(request, response, capturingChain);
        assertNull(MDC.get("requestId"));
    }

    @Test
    void doFilterInternal_ShouldGenerateRequestId_WhenHeaderMissing() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getQueryString()).thenReturn("q=1");
        when(response.getStatus()).thenReturn(201);

        FilterChain capturingChain = (req, res) -> {
            assertNotNull(MDC.get("requestId"));
            assertFalse(MDC.get("requestId").isBlank());
        };

        filter.doFilterInternal(request, response, capturingChain);
        assertNull(MDC.get("requestId"));
    }

    @Test
    void doFilterInternal_ShouldGenerateRequestId_WhenHeaderBlank() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("");

        FilterChain capturingChain = (req, res) -> {
            assertNotNull(MDC.get("requestId"));
            assertFalse(MDC.get("requestId").isBlank());
        };

        filter.doFilterInternal(request, response, capturingChain);
        assertNull(MDC.get("requestId"));
    }

    @Test
    void doFilterInternal_ShouldClearMDC_AfterChain() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("req-123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        when(response.getStatus()).thenReturn(200);

        filter.doFilterInternal(request, response, (req, res) -> {});
        assertNull(MDC.get("requestId"));
        assertNull(MDC.get("instanceId"));
    }
}
