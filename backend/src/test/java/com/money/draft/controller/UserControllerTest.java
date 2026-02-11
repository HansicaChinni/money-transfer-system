
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                //.setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        SecurityContextHolder.clearContext();
    }

    @Test
    void transfer_ShouldReturnSuccess_WhenTransferIsValid() throws Exception {
        // Given
        setSecurityContext("john.doe");
        AppUser user = createAppUser(1L, "john.doe", 100L);
        MeTransferRequest request = new MeTransferRequest(200L, new BigDecimal("50.00"));
        TransferResponse expectedResponse = TransferResponse.success(1L, new BigDecimal("50.00"));

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(transferService.transferForUser(100L, 200L, new BigDecimal("50.00")))
                .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/me/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.transactionId", is(1)))
                .andExpect(jsonPath("$.amount", is(50.00)));

        verify(appUserRepository, times(1)).findByUsername("john.doe");
        verify(transferService, times(1)).transferForUser(100L, 200L, new BigDecimal("50.00"));
    }

    @Test
    void transfer_ShouldReturn404_WhenUserAccountNotFound() throws Exception {
        // Given
        setSecurityContext("john.doe");
        MeTransferRequest request = new MeTransferRequest(200L, new BigDecimal("50.00"));

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/me/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(appUserRepository, times(1)).findByUsername("john.doe");
        verify(transferService, never()).transferForUser(anyLong(), anyLong(), any());
    }

    @Test
    void transfer_ShouldReturn400_WhenAmountIsNegative() throws Exception {
        // Given
        setSecurityContext("john.doe");
        MeTransferRequest request = new MeTransferRequest(200L, new BigDecimal("-10.00"));

        // When & Then
        mockMvc.perform(post("/me/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transferForUser(anyLong(), anyLong(), any());
    }

    @Test
    void transfer_ShouldReturn400_WhenToAccountIdIsNull() throws Exception {
        // Given
        setSecurityContext("john.doe");
        MeTransferRequest request = new MeTransferRequest(null, new BigDecimal("50.00"));

        // When & Then
        mockMvc.perform(post("/me/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transferForUser(anyLong(), anyLong(), any());
    }

    @Test
    void transfer_ShouldReturn400_WhenInsufficientBalance() throws Exception {
        // Given
        setSecurityContext("john.doe");
        AppUser user = createAppUser(1L, "john.doe", 100L);
        MeTransferRequest request = new MeTransferRequest(200L, new BigDecimal("5000.00"));

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(transferService.transferForUser(100L, 200L, new BigDecimal("5000.00")))
                .thenThrow(new InsufficientBalanceException(
                        100L,
                        new BigDecimal("1000.00"),   // example current balance
                        new BigDecimal("5000.00")    // attempted
                ));

        // When & Then
        mockMvc.perform(post("/me/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferService, times(1)).transferForUser(100L, 200L, new BigDecimal("5000.00"));
    }

    @Test
    void getBalance_ShouldReturnAccountDetails() throws Exception {
        // Given
        setSecurityContext("john.doe");
        AppUser user = createAppUser(1L, "john.doe", 100L);
        AccountResponse accountResponse = new AccountResponse(
                100L,
                "John Doe",
                new BigDecimal("1500.00"),
                "ACTIVE"
        );

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(accountService.getAccount(100L)).thenReturn(accountResponse);

        // When & Then
        mockMvc.perform(get("/me/balance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.holderName", is("John Doe")))
                .andExpect(jsonPath("$.balance", is(1500.00)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(appUserRepository, times(1)).findByUsername("john.doe");
        verify(accountService, times(1)).getAccount(100L);
    }

    @Test
    void getBalance_ShouldReturn404_WhenUserAccountNotFound() throws Exception {
        // Given
        setSecurityContext("john.doe");

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/me/balance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(appUserRepository, times(1)).findByUsername("john.doe");
        verify(accountService, never()).getAccount(anyLong());
    }

    @Test
    void getTransactions_ShouldReturnTransactionHistory() throws Exception {
        // Given
        setSecurityContext("john.doe");
        AppUser user = createAppUser(1L, "john.doe", 100L);
        Instant now = Instant.now();

        List<TransactionLogResponse> transactions = Arrays.asList(
                new TransactionLogResponse(1L, 100L, 200L, new BigDecimal("50.00"),
                        "SUCCESS", null, "key-1", now),
                new TransactionLogResponse(2L, 200L, 100L, new BigDecimal("30.00"),
                        "SUCCESS", null, "key-2", now),
                new TransactionLogResponse(3L, 100L, 300L, new BigDecimal("20.00"),
                        "FAILED", "Insufficient funds", "key-3", now)
        );

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(accountService.getTransactions(100L)).thenReturn(transactions);

        // When & Then
        mockMvc.perform(get("/me/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].fromAccountId", is(100)))
                .andExpect(jsonPath("$[0].toAccountId", is(200)))
                .andExpect(jsonPath("$[0].amount", is(50.00)))
                .andExpect(jsonPath("$[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].amount", is(30.00)))
                .andExpect(jsonPath("$[2].status", is("FAILED")))
                .andExpect(jsonPath("$[2].failureReason", is("Insufficient funds")));

        verify(appUserRepository, times(1)).findByUsername("john.doe");
        verify(accountService, times(1)).getTransactions(100L);
    }

    @Test
    void getTransactions_ShouldReturnEmptyList_WhenNoTransactions() throws Exception {
        // Given
        setSecurityContext("john.doe");
        AppUser user = createAppUser(1L, "john.doe", 100L);

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(user));
        when(accountService.getTransactions(100L)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/me/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(accountService, times(1)).getTransactions(100L);
    }

    @Test
    void getTransactions_ShouldReturn404_WhenUserAccountNotFound() throws Exception {
        // Given
        setSecurityContext("john.doe");

        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/me/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(appUserRepository, times(1)).findByUsername("john.doe");
        verify(accountService, never()).getTransactions(anyLong());
    }

    // Helper methods
    private void setSecurityContext(String username) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private AppUser createAppUser(Long id, String username, Long accountId) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setRole(Role.USER);
        user.setAccountId(accountId);
        user.setCreatedAt(Instant.now());
        return user;
    }
}
