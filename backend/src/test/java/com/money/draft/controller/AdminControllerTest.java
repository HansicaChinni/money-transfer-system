
package com.money.draft.controller;

import com.money.draft.dto.AdminAccountView;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.service.AdminService;
import com.money.draft.service.CaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;   // <-- IMPORTANT: use LocalDateTime, not Instant
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private CaptchaService captchaService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    void allAccountsNoNames_ShouldReturnListOfAccounts() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        List<AdminAccountView> mockAccounts = Arrays.asList(
                new AdminAccountView(1L, "ACC-2026-000001", new BigDecimal("1000.00"), "ACTIVE", now),
                new AdminAccountView(2L, "ACC-2026-000002", new BigDecimal("2500.50"), "ACTIVE", now),
                new AdminAccountView(3L, "ACC-2026-000003", new BigDecimal("500.75"), "LOCKED", now)
        );

        when(adminService.getAllAccounts()).thenReturn(mockAccounts);

        mockMvc.perform(get("/admin/accounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].balance", is(1000.00)))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].balance", is(2500.50)))
                .andExpect(jsonPath("$[1].status", is("ACTIVE")))
                .andExpect(jsonPath("$[2].id", is(3)))
                .andExpect(jsonPath("$[2].balance", is(500.75)))
                .andExpect(jsonPath("$[2].status", is("LOCKED")));

        verify(adminService, times(1)).getAllAccounts();
    }

    @Test
    void allAccountsNoNames_ShouldReturnEmptyList_WhenNoAccounts() throws Exception {
        when(adminService.getAllAccounts()).thenReturn(List.of());

        mockMvc.perform(get("/admin/accounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(adminService, times(1)).getAllAccounts();
    }

    @Test
    void allTransactions_ShouldReturnListOfTransactions() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        List<TransactionLogResponse> mockTransactions = Arrays.asList(
                new TransactionLogResponse(1L, 1L, 2L, new BigDecimal("500.00"),
                        "SUCCESS", null, "key-1", now),
                new TransactionLogResponse(2L, 2L, 3L, new BigDecimal("200.00"),
                        "SUCCESS", null, "key-2", now),
                new TransactionLogResponse(3L, 1L, 3L, new BigDecimal("300.00"),
                        "FAILED", "Insufficient funds", "key-3", now)
        );

        when(adminService.getAllTransactions()).thenReturn(mockTransactions);

        mockMvc.perform(get("/admin/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].fromAccountId", is(1)))
                .andExpect(jsonPath("$[0].toAccountId", is(2)))
                .andExpect(jsonPath("$[0].amount", is(500.00)))
                .andExpect(jsonPath("$[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$[0].idempotencyKey", is("key-1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].amount", is(200.00)))
                .andExpect(jsonPath("$[1].status", is("SUCCESS")))
                .andExpect(jsonPath("$[2].id", is(3)))
                .andExpect(jsonPath("$[2].amount", is(300.00)))
                .andExpect(jsonPath("$[2].status", is("FAILED")))
                .andExpect(jsonPath("$[2].failureReason", is("Insufficient funds")));

        verify(adminService, times(1)).getAllTransactions();
    }

    @Test
    void allTransactions_ShouldReturnEmptyList_WhenNoTransactions() throws Exception {
        when(adminService.getAllTransactions()).thenReturn(List.of());

        mockMvc.perform(get("/admin/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(adminService, times(1)).getAllTransactions();
    }

    @Test
    void allAccountsNoNames_ShouldHandleMultipleAccountStatuses() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        List<AdminAccountView> mockAccounts = Arrays.asList(
                new AdminAccountView(1L, "ACC-2026-000001", new BigDecimal("1000.00"), "ACTIVE", now),
                new AdminAccountView(2L, "ACC-2026-000002", new BigDecimal("2500.50"), "LOCKED", now),
                new AdminAccountView(3L, "ACC-2026-000003", new BigDecimal("0.00"), "CLOSED", now)
        );

        when(adminService.getAllAccounts()).thenReturn(mockAccounts);

        mockMvc.perform(get("/admin/accounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$[1].status", is("LOCKED")))
                .andExpect(jsonPath("$[2].status", is("CLOSED")));

        verify(adminService, times(1)).getAllAccounts();
    }

    @Test
    void allTransactions_ShouldHandleSuccessAndFailedTransactions() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        List<TransactionLogResponse> mockTransactions = Arrays.asList(
                new TransactionLogResponse(1L, 1L, 2L, new BigDecimal("100.00"),
                        "SUCCESS", null, "success-key", now),
                new TransactionLogResponse(2L, 3L, 4L, new BigDecimal("50.00"),
                        "FAILED", "Account not active", "failed-key", now)
        );

        when(adminService.getAllTransactions()).thenReturn(mockTransactions);

        mockMvc.perform(get("/admin/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status", is("SUCCESS")))
                // If your ObjectMapper includes nulls, replace with: .andExpect(jsonPath("$[0].failureReason").value((Object) null))
                .andExpect(jsonPath("$[0].failureReason").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[1].status", is("FAILED")))
                .andExpect(jsonPath("$[1].failureReason", is("Account not active")));

        verify(adminService, times(1)).getAllTransactions();
    }
}
