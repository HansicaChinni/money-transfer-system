package com.money.draft.security;

import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.enums.Role;
import com.money.draft.domain.repository.AppUserRepository;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AppUserRepository userRepo;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new JwtAuthFilter(jwtService, userRepo);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_validToken_setsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token123");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        AppUser user = new AppUser();
        user.setUsername("test1");
        user.setRole(Role.USER);

        Jws<Claims> jws = mock(Jws.class);
        Claims claims = mock(Claims.class);

        when(jwtService.parse("token123")).thenReturn(jws);
        when(jws.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn("test1");
        when(userRepo.findByUsername("test1"))
                .thenReturn(Optional.of(user));

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("test1", auth.getPrincipal());
    }

    @Test
    void doFilter_invalidToken_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad");

        when(jwtService.parse("bad"))
                .thenThrow(new RuntimeException("bad token"));

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotFilter_authEndpoint() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/auth/login");

        assertTrue(filter.shouldNotFilter(request));
    }
}
