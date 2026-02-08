package com.money.draft.controller;

import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.enums.Role;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.LoginRequest;
import com.money.draft.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    // ---------------- SUCCESS CASE ----------------

    @Test
    void login_success() throws Exception {
        AppUser user = new AppUser();
        user.setUsername("jiya");
        user.setPassword("encodedPass");
        user.setRole(Role.USER);
        user.setAccountId(10L);

        when(userRepository.findByUsername("jiya"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPass"))
                .thenReturn(true);
        when(jwtService.generateToken("jiya", "USER", 10L))
                .thenReturn("jwt-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "username": "jiya",
                      "password": "password"
                    }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accountId").value(10));
    }

    // ---------------- FAILURE CASE ----------------

    @Test
    void login_invalidCredentials() throws Exception {
        when(userRepository.findByUsername("wrong"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "username": "wrong",
                      "password": "wrong"
                    }
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }
}

