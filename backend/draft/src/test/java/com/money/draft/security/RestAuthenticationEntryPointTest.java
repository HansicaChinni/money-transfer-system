package com.money.draft.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RestAuthenticationEntryPointTest {

    private final RestAuthenticationEntryPoint entryPoint =
            new RestAuthenticationEntryPoint();

    @Test
    void commence_returns401Json() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/me/transfer");
        MockHttpServletResponse res = new MockHttpServletResponse();

        entryPoint.commence(req, res, mock(AuthenticationException.class));

        assertEquals(401, res.getStatus());
        assertTrue(res.getContentAsString().contains("UNAUTHORIZED"));
    }
}
