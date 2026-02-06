
package com.money.draft.security;

import com.money.draft.domain.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserRepository userRepo;

    public JwtAuthFilter(JwtService jwtService, AppUserRepository userRepo) {
        this.jwtService = jwtService;
        this.userRepo = userRepo;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // ✅ Skip auth endpoints and Swagger docs
        return path.startsWith("/auth/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String auth = request.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                var jws = jwtService.parse(token);
                String username = jws.getBody().getSubject();
                var opt = userRepo.findByUsername(username);
                if (opt.isPresent()) {
                    var u = opt.get();
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
                    var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // Do NOT write to response; let chain continue.
            }
        }

        filterChain.doFilter(request, response);
    }
}
