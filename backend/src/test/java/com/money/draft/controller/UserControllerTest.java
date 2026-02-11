
package com.money.draft.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.enums.Role;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.AccountResponse;
import com.money.draft.dto.MeTransferRequest;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.dto.TransferResponse;
import com.money.draft.exception.InsufficientBalanceException;
import com.money.draft.service.AccountService;
import com.money.draft.service.TransferService;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private TransferService transferService;

    @Mock
    private AccountService accountService;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
        SecurityContextHolder.clearContext();
    }

    /* -------------------- TRANSFER TESTS -------------------- */

    @Test
    void transfer_ShouldReturnSuccess_WhenTransferIsValid() throws Exception {
        setSecurityContext("john.doe");
        AppUser user = createUser(1L, "john.doe", 100L);

        MeTransferRequest req = new MeTransferRequest(200L, new BigDecimal("50.00"));
        TransferResponse res = TransferResponse.success(1L, new BigDecimal("50.00"));

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(transferService.transferForUser(100L, 200L, new BigDecimal("50.00")))
                .thenReturn(res);

        mockMvc.perform(post("/me/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.transactionId", is(1)))
                .andExpect(jsonPath("$.amount", is(50.00)));
    }

    @Test
    void transfer_ShouldReturn404_WhenUserAccountNotFound() throws Exception {
        setSecurityContext("john.doe");

        MeTransferRequest req = new MeTransferRequest(200L, new BigDecimal("50.00"));
        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.empty());

        mockMvc.perform(post("/me/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_ShouldReturn400_WhenAmountIsNegative() throws Exception {
        setSecurityContext("john.doe");
        MeTransferRequest req = new MeTransferRequest(200L, new BigDecimal("-10"));

        mockMvc.perform(post("/me/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_ShouldReturn400_WhenInsufficientBalance() throws Exception {
        setSecurityContext("john.doe");
        AppUser user = createUser(1L, "john.doe", 100L);
        MeTransferRequest req = new MeTransferRequest(200L, new BigDecimal("5000"));

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(transferService.transferForUser(anyLong(), anyLong(), any()))
                .thenThrow(new InsufficientBalanceException(100L,
                        new BigDecimal("100"), new BigDecimal("5000")));

        mockMvc.perform(post("/me/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    /* -------------------- BALANCE TESTS -------------------- */

    @Test
    void getBalance_ShouldReturnAccountDetails() throws Exception {
        setSecurityContext("john.doe");
        AppUser user = createUser(1L, "john.doe", 100L);

        AccountResponse response = new AccountResponse(
                100L, "John", new BigDecimal("1500"), "ACTIVE"
        );

        when(appUserRepository.findByUsername("john.doe"))
                .thenReturn(Optional.of(user));
        when(accountService.getAccount(100L)).thenReturn(response);

        mockMvc.perform(get("/me/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.balance", is(1500.00)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void getBalance_ShouldReturn404_WhenUserAccountNotFound() throws Exception {
        setSecurityContext("john.doe");
        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.empty());

        mockMvc.perform(get("/me/balance"))
                .andExpect(status().isNotFound());
    }

    /* -------------------- TRANSACTION HISTORY TESTS -------------------- */

    @Test
    void getTransactions_ShouldReturnTransactionHistory() throws Exception {
        setSecurityContext("john.doe");
        AppUser user = createUser(1L, "john.doe", 100L);

        LocalDateTime now = LocalDateTime.now();

        List<TransactionLogResponse> logs = Arrays.asList(
                new TransactionLogResponse(1L, 100L, 200L, new BigDecimal("50"),
                        "SUCCESS", null, "A", now),
                new TransactionLogResponse(2L, 200L, 100L, new BigDecimal("30"),
                        "SUCCESS", null, "B", now),
                new TransactionLogResponse(3L, 100L, 300L, new BigDecimal("20"),
                        "FAILED", "Insufficient funds", "C", now)
        );

        when(appUserRepository.findByUsername("john.doe"))
                .thenReturn(Optional.of(user));
        when(accountService.getTransactions(100L)).thenReturn(logs);

        mockMvc.perform(get("/me/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[2].status", is("FAILED")));
    }

    @Test
    void getTransactions_ShouldReturnEmptyList_WhenNoTransactions() throws Exception {
        setSecurityContext("john.doe");
        AppUser user = createUser(1L, "john.doe", 100L);

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(accountService.getTransactions(100L)).thenReturn(List.of());

        mockMvc.perform(get("/me/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getTransactions_ShouldReturn404_WhenUserAccountNotFound() throws Exception {
        setSecurityContext("john.doe");

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.empty());

        mockMvc.perform(get("/me/transactions"))
                .andExpect(status().isNotFound());
    }

    /* -------------------- HELPERS -------------------- */

    private void setSecurityContext(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    private AppUser createUser(Long id, String username, Long accountId) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setUsername(username);
        u.setRole(Role.USER);
        u.setAccountId(accountId);
        u.setCreatedAt(Instant.now());
        return u;
    }
}
