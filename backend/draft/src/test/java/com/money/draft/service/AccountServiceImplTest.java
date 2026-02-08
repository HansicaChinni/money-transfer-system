package com.money.draft.service;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.AccountStatus;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.exception.AccountNotFoundException;
import com.money.draft.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepo;

    @Mock
    private TransactionLogRepository txRepo;

    @InjectMocks
    private AccountServiceImpl service;

    @Test
    void getAccount_success() {
        Account account = new Account();
        account.setId(1L);
        account.setHolderName("A");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setStatus(AccountStatus.ACTIVE);

        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

        var response = service.getAccount(1L);

        assertEquals(1L, response.getId());
        assertEquals(BigDecimal.valueOf(1000), response.getBalance());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void getAccount_notFound() {
        when(accountRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.getAccount(1L));
    }

    @Test
    void getBalance_success() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(BigDecimal.valueOf(500));

        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

        BigDecimal balance = service.getBalance(1L);

        assertEquals(BigDecimal.valueOf(500), balance);
    }

    @Test
    void getTransactions_filtersAndSorts() throws Exception {
        TransactionLog older =
                TransactionLog.success(1L, 2L, BigDecimal.TEN, "k1");

        // Ensure different timestamps
        Thread.sleep(5);

        TransactionLog newer =
                TransactionLog.success(2L, 1L, BigDecimal.ONE, "k2");

        when(txRepo.findAll()).thenReturn(List.of(older, newer));

        List<TransactionLogResponse> result = service.getTransactions(1L);

        assertEquals(2, result.size());
        assertEquals("k2", result.get(0).getIdempotencyKey()); // newest first
        assertEquals("k1", result.get(1).getIdempotencyKey());
    }

}

