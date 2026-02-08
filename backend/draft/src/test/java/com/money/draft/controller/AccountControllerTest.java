package com.money.draft.controller;

import com.money.draft.dto.AccountResponse;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void getAccount_success() throws Exception {
        AccountResponse response = new AccountResponse();
        response.setId(1L);
        response.setHolderName("test1");
        response.setBalance(BigDecimal.valueOf(1000));
        response.setStatus("ACTIVE");

        when(accountService.getAccount(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.balance").value(1000));
    }

    @Test
    void getBalance_success() throws Exception {
        when(accountService.getBalance(1L))
                .thenReturn(BigDecimal.valueOf(500));

        mockMvc.perform(get("/api/v1/accounts/1/balance"))
                .andExpect(status().isOk())
                .andExpect(content().string("500"));
    }

    @Test
    void getTransactions_success() throws Exception {
        TransactionLogResponse tx = new TransactionLogResponse();
        tx.setId(1L);
        tx.setFromAccountId(1L);
        tx.setToAccountId(2L);
        tx.setAmount(BigDecimal.valueOf(100));
        tx.setStatus("SUCCESS");
        tx.setIdempotencyKey("abc123");
        tx.setCreatedOn(Instant.now());

        when(accountService.getTransactions(1L))
                .thenReturn(List.of(tx));

        mockMvc.perform(get("/api/v1/accounts/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(100))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }


}

