package com.money.draft.controller;


import com.money.draft.domain.entity.Account;
import com.money.draft.domain.enums.AccountStatus;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private TransactionLogRepository transactionLogRepository;

    @Test
    void allAccountsNoNames_success() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(BigDecimal.valueOf(2500));
        account.setStatus(AccountStatus.ACTIVE);
        account.setLastUpdated(Instant.now());

        when(accountRepository.findAll()).thenReturn(List.of(account));

        mockMvc.perform(get("/admin/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].balance").value(2500))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void allTransactions_success() throws Exception {
        when(transactionLogRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

