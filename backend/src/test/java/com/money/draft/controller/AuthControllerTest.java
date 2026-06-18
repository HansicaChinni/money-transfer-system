package com.money.draft.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.enums.Role;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.LoginRequest;
import com.money.draft.security.JwtService;
import com.money.draft.service.CaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CaptchaService captchaService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
        lenient().when(captchaService.validateCaptcha(any(), any())).thenReturn(true);
    }


    @Test
    void login_ShouldReturnTokenAndUserDetails_WhenCredentialsAreValid() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("john.doe", "password123");
        AppUser user = createAppUser(1L, "john.doe", "password123", Role.USER, 100L);
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        when(appUserRepository.findByUsername("john.doe"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password123", "password123"))
                .thenReturn(true);

        // JwtService expects (String username, String role, Long accountId)
        when(jwtService.generateToken(eq("john.doe"), eq(Role.USER.name()), eq(100L)))
                .thenReturn(token);

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is(token)))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.accountId", is(100)));

        verify(appUserRepository, times(1)).findByUsername("john.doe");
        verify(passwordEncoder, times(1)).matches("password123", "password123");

        verify(jwtService, times(1))
                .generateToken(eq("john.doe"), eq(Role.USER.name()), eq(100L));
    }



    @Test
    void login_ShouldReturnTokenWithNullAccountId_WhenUserIsAdmin() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");
        AppUser admin = createAppUser(2L, "admin", "admin123", Role.ADMIN, null);
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...admin";

        when(appUserRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));

        when(passwordEncoder.matches("admin123", "admin123"))
                .thenReturn(true);

        // accountId = null → match using isNull()
        when(jwtService.generateToken(eq("admin"), eq(Role.ADMIN.name()), isNull()))
                .thenReturn(token);

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is(token)))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.accountId").doesNotExist());

        verify(appUserRepository, times(1)).findByUsername("admin");
        verify(passwordEncoder, times(1)).matches("admin123", "admin123");

        verify(jwtService, times(1))
                .generateToken(eq("admin"), eq(Role.ADMIN.name()), isNull());
    }


    @Test
    void login_ShouldReturn401_WhenUserNotFound() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("nonexistent", "password");

        when(appUserRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        verify(appUserRepository, times(1)).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyString(), any(), any());
    }

    @Test
    void login_ShouldReturn401_WhenPasswordIsIncorrect() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("john.doe", "wrongpassword");
        AppUser user = createAppUser(1L, "john.doe", "password123", Role.USER, 100L);

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "password123")).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        verify(appUserRepository, times(1)).findByUsername("john.doe");
        verify(passwordEncoder, times(1)).matches("wrongpassword", "password123");
        verify(jwtService, never()).generateToken(anyString(), any(), any());
    }

    @Test
    void login_ShouldReturn400_WhenUsernameIsBlank() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("", "password123");

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(appUserRepository, never()).findByUsername(anyString());
    }

    @Test
    void login_ShouldReturn400_WhenPasswordIsBlank() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("john.doe", "");

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(appUserRepository, never()).findByUsername(anyString());
    }

    @Test
    void login_ShouldReturn400_WhenRequestBodyIsInvalid() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(appUserRepository, never()).findByUsername(anyString());
    }

    // Helper method
    private AppUser createAppUser(Long id, String username, String password, Role role, Long accountId) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setAccountId(accountId);
        user.setCreatedAt(Instant.now());
        return user;
    }
}