
package com.money.draft.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.enums.Role;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.*;
import com.money.draft.exception.GlobalExceptionHandler;
import com.money.draft.service.RewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserRewardControllerTest {

    @Mock
    private RewardService rewardService;

    @Mock
    private AppUserRepository userRepo;

    @InjectMocks
    private UserRewardController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    private AppUser createUser(Long accountId) {
        AppUser u = new AppUser();
        u.setId(1L);
        u.setUsername("john");
        u.setRole(Role.USER);
        u.setAccountId(accountId);
        u.setCreatedAt(Instant.now());
        return u;
    }

    @Test
    void getSummary_ShouldReturnSummary() throws Exception {
        setSecurityContext("john");
        when(userRepo.findByUsername("john")).thenReturn(Optional.of(createUser(100L)));
        when(rewardService.getRewardSummary(100L)).thenReturn(new RewardSummaryResponse(50, 10, "2026-06-17T12:00:00"));

        mockMvc.perform(get("/api/user/rewards/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints", is(50)))
                .andExpect(jsonPath("$.lastEarned", is(10)))
                .andExpect(jsonPath("$.lastEarnedOn", is("2026-06-17T12:00:00")));
    }

    @Test
    void getSummary_ShouldReturn400_WhenUserNotFound() throws Exception {
        setSecurityContext("john");
        when(userRepo.findByUsername("john")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/user/rewards/summary"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_ShouldReturnHistory() throws Exception {
        setSecurityContext("john");
        when(userRepo.findByUsername("john")).thenReturn(Optional.of(createUser(100L)));
        when(rewardService.getRewardHistory(100L)).thenReturn(List.of(
                new RewardTransactionResponse(1L, 10L, 5, new BigDecimal("1000"), "Transferred ₹1000 → 5 points", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/user/rewards/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].pointsEarned", is(5)));
    }

    @Test
    void getStore_ShouldReturnItems() throws Exception {
        when(rewardService.getAvailableItems()).thenReturn(List.of(
                new RewardItemResponse(1L, "Gift", "desc", "Brand", 100, new BigDecimal("50"), true, "img.jpg")
        ));

        mockMvc.perform(get("/api/user/rewards/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Gift")));
    }

    @Test
    void redeem_ShouldSucceed() throws Exception {
        setSecurityContext("john");
        when(userRepo.findByUsername("john")).thenReturn(Optional.of(createUser(100L)));

        var req = new RedeemRequest(1L);
        var resp = new RedemptionResponse(10L, 1L, "Gift", "Brand", 100, new BigDecimal("50"),
                "PENDING", null, null, Instant.now(), null);

        when(rewardService.redeem(100L, 1L)).thenReturn(resp);

        mockMvc.perform(post("/api/user/rewards/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void redeem_ShouldReturn400_WhenUserNotFound() throws Exception {
        setSecurityContext("john");
        when(userRepo.findByUsername("john")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/user/rewards/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemRequest(1L))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRedemptions_ShouldReturnList() throws Exception {
        setSecurityContext("john");
        when(userRepo.findByUsername("john")).thenReturn(Optional.of(createUser(100L)));
        when(rewardService.getRedemptions(100L)).thenReturn(List.of(
                new RedemptionResponse(10L, 1L, "Gift", "Brand", 100, new BigDecimal("50"),
                        "PENDING", null, null, Instant.now(), null)
        ));

        mockMvc.perform(get("/api/user/rewards/redemptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(10)));
    }

    @Test
    void endpoints_ShouldReturn400_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user/rewards/summary"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/user/rewards/history"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/user/rewards/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemRequest(1L))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/user/rewards/redemptions"))
                .andExpect(status().isBadRequest());
    }
}
