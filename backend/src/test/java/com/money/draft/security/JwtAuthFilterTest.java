
package com.money.draft.security;

import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.enums.Role;
import com.money.draft.domain.repository.AppUserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private AppUserRepository userRepo;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter_ShouldReturnTrue_ForAuthPaths() {
        when(request.getRequestURI()).thenReturn("/auth/login");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_ShouldReturnTrue_ForSwaggerPaths() {
        when(request.getRequestURI()).thenReturn("/v3/api-docs");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_ShouldReturnTrue_ForSwaggerUi() {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_ShouldReturnFalse_ForOtherPaths() {
        when(request.getRequestURI()).thenReturn("/me/balance");
        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void doFilterInternal_ShouldSetAuthentication_WhenValidToken() throws Exception {
        AppUser user = new AppUser();
        user.setUsername("john");
        user.setRole(Role.USER);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.parse("valid-token")).thenAnswer(inv -> {
            var claims = io.jsonwebtoken.Jwts.claims();
            claims.setSubject("john");
            var jws = mock(io.jsonwebtoken.Jws.class);
            when(jws.getBody()).thenReturn(claims);
            return jws;
        });
        when(userRepo.findByUsername("john")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("john", auth.getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        filter.doFilterInternal(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenInvalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtService.parse("bad-token")).thenThrow(new JwtException("bad"));

        filter.doFilterInternal(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenUserNotFound() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.parse("valid-token")).thenAnswer(inv -> {
            var claims = io.jsonwebtoken.Jwts.claims();
            claims.setSubject("unknown");
            var jws = mock(io.jsonwebtoken.Jws.class);
            when(jws.getBody()).thenReturn(claims);
            return jws;
        });
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldHandleNonBearerHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic somecreds");

        filter.doFilterInternal(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
