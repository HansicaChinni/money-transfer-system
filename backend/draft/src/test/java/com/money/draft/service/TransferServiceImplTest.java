package com.money.draft.service;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.AccountStatus;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.TransferRequest;
import com.money.draft.dto.TransferResponse;
import com.money.draft.exception.*;
import com.money.draft.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private AccountRepository accountRepo;

    @Mock
    private TransactionLogRepository txRepo;

    @Mock
    private TransactionLogWriter logWriter;

    @InjectMocks
    private TransferServiceImpl service;

    @Test
    void transfer_success() {
        Account from = new Account();
        from.setId(1L);
        from.setBalance(BigDecimal.valueOf(500));
        from.setStatus(AccountStatus.ACTIVE);

        Account to = new Account();
        to.setId(2L);
        to.setBalance(BigDecimal.valueOf(100));
        to.setStatus(AccountStatus.ACTIVE);

        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));
        when(logWriter.logSuccess(any(), any(), any(), any()))
                .thenReturn(TransactionLog.success(1L, 2L, BigDecimal.valueOf(100), "k1"));

        TransferRequest req = new TransferRequest();
        req.setFromAccountId(1L);
        req.setToAccountId(2L);
        req.setAmount(BigDecimal.valueOf(100));
        req.setIdempotencyKey("k1");

        TransferResponse response = service.transfer(req);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(BigDecimal.valueOf(100), response.getAmount());
    }


    @Test
    void transfer_selfTransferNotAllowed() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountId(1L);
        req.setToAccountId(1L);
        req.setAmount(BigDecimal.TEN);
        req.setIdempotencyKey("k");

        assertThrows(SelfTransferNotAllowedException.class,
                () -> service.transfer(req));
    }

    @Test
    void transfer_insufficientBalance() {
        Account from = new Account();
        from.setId(1L);
        from.setBalance(BigDecimal.valueOf(10));
        from.setStatus(AccountStatus.ACTIVE);

        Account to = new Account();
        to.setId(2L);
        to.setBalance(BigDecimal.ZERO);
        to.setStatus(AccountStatus.ACTIVE);

        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));

        TransferRequest req = new TransferRequest();
        req.setFromAccountId(1L);
        req.setToAccountId(2L);
        req.setAmount(BigDecimal.valueOf(100));
        req.setIdempotencyKey("k");

        assertThrows(InsufficientBalanceException.class,
                () -> service.transfer(req));
    }

    @Test
    void transfer_duplicateIdempotencyKey() {
        TransactionLog existing =
                TransactionLog.success(1L, 2L, BigDecimal.TEN, "dup");

        when(txRepo.findByIdempotencyKey("dup"))
                .thenReturn(Optional.of(existing));

        TransferRequest req = new TransferRequest();
        req.setFromAccountId(1L);
        req.setToAccountId(2L);
        req.setAmount(BigDecimal.TEN);
        req.setIdempotencyKey("dup");

        assertThrows(DuplicateTransferException.class,
                () -> service.transfer(req));
    }

}
