package com.money.draft.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class RestAccessDeniedHandlerTest {

    private final RestAccessDeniedHandler handler =
            new RestAccessDeniedHandler();

    @Test
    void handle_returns403Json() throws Exception {
        MockHttpServletRequest req =
                new MockHttpServletRequest("GET", "/admin/accounts");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.handle(req, res, new AccessDeniedException("denied"));

        assertEquals(403, res.getStatus());
        assertTrue(res.getContentAsString().contains("FORBIDDEN"));
    }
}
