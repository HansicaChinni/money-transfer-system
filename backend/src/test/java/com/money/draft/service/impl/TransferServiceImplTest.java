
package com.money.draft.service.impl;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.AccountStatus;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.TransferRequest;
import com.money.draft.dto.TransferResponse;
import com.money.draft.exception.*;
import com.money.draft.service.RewardService;
import com.money.draft.service.TransactionLogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private AccountRepository accountRepo;
    @Mock
    private TransactionLogRepository txRepo;
    @Mock
    private TransactionLogWriter logWriter;
    @Mock
    private RewardService rewardService;

    @InjectMocks
    private TransferServiceImpl transferService;

    private Account createAccount(Long id, BigDecimal balance, AccountStatus status) {
        Account a = new Account();
        a.setId(id);
        a.setBalance(balance);
        a.setStatus(status);
        return a;
    }

    /* ---------- transfer ---------- */

    @Test
    void transfer_ShouldSucceed() {
        Account from = createAccount(1L, new BigDecimal("1000"), AccountStatus.ACTIVE);
        Account to = createAccount(2L, new BigDecimal("500"), AccountStatus.ACTIVE);
        TransactionLog txLog = TransactionLog.success(1L, 2L, new BigDecimal("200"), "key-1");

        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));
        when(logWriter.logSuccess(1L, 2L, new BigDecimal("200"), "key-1")).thenReturn(txLog);

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("200"), "key-1");
        TransferResponse result = transferService.transfer(req);

        assertEquals("SUCCESS", result.status());
        assertEquals(new BigDecimal("200"), result.amount());
        verify(rewardService).grantRewardIfEligible(1L, 2L, txLog.getId(), new BigDecimal("200"));
    }

    @Test
    void transfer_ShouldThrow_WhenRequestIsNull() {
        assertThrows(ValidationException.class, () -> transferService.transfer(null));
    }

    @Test
    void transfer_ShouldThrow_WhenDuplicateKey() {
        when(txRepo.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(mock(TransactionLog.class)));

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100"), "dup-key");
        assertThrows(DuplicateTransferException.class, () -> transferService.transfer(req));
    }

    @Test
    void transfer_ShouldThrow_WhenSelfTransfer() {
        TransferRequest req = new TransferRequest(1L, 1L, new BigDecimal("100"), "key-1");
        assertThrows(SelfTransferNotAllowedException.class, () -> transferService.transfer(req));
    }

    @Test
    void transfer_ShouldGenerateIdempotencyKey_WhenBlank() {
        Account from = createAccount(1L, new BigDecimal("1000"), AccountStatus.ACTIVE);
        Account to = createAccount(2L, new BigDecimal("500"), AccountStatus.ACTIVE);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));
        when(logWriter.logSuccess(anyLong(), anyLong(), any(), anyString())).thenReturn(mock(TransactionLog.class));

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100"), "");
        TransferResponse result = transferService.transfer(req);

        assertEquals("SUCCESS", result.status());
    }

    @Test
    void transfer_ShouldHandleOptimisticLockingRetry() {
        Account from = createAccount(1L, new BigDecimal("1000"), AccountStatus.ACTIVE);
        Account to = createAccount(2L, new BigDecimal("500"), AccountStatus.ACTIVE);

        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));

        when(logWriter.logSuccess(anyLong(), anyLong(), any(), anyString()))
                .thenThrow(new OptimisticLockingFailureException("lock"))
                .thenReturn(mock(TransactionLog.class));

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100"), "key-1");
        TransferResponse result = transferService.transfer(req);

        assertEquals("SUCCESS", result.status());
        verify(accountRepo, times(2)).findById(1L);
    }

    @Test
    void transfer_ShouldThrowOptimisticLock_AfterMaxRetries() {
        Account from = createAccount(1L, new BigDecimal("1000"), AccountStatus.ACTIVE);
        Account to = createAccount(2L, new BigDecimal("500"), AccountStatus.ACTIVE);

        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));
        when(logWriter.logSuccess(anyLong(), anyLong(), any(), anyString()))
                .thenThrow(new OptimisticLockingFailureException("lock"));

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100"), "key-1");
        assertThrows(OptimisticLockingFailureException.class, () -> transferService.transfer(req));
    }

    @Test
    void transfer_ShouldLogFailure_WhenBusinessExceptionThrown() {
        Account from = createAccount(1L, new BigDecimal("10"), AccountStatus.ACTIVE);
        Account to = createAccount(2L, new BigDecimal("500"), AccountStatus.ACTIVE);

        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));

        when(logWriter.logFailure(anyLong(), anyLong(), any(), anyString(), anyString()))
                .thenReturn(mock(TransactionLog.class));

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100"), "key-1");
        assertThrows(InsufficientBalanceException.class, () -> transferService.transfer(req));
        verify(logWriter).logFailure(eq(1L), eq(2L), eq(new BigDecimal("100")), eq("key-1"), anyString());
    }

    @Test
    void transfer_ShouldNotThrow_WhenLogFailureFails() {
        Account from = createAccount(1L, new BigDecimal("10"), AccountStatus.ACTIVE);
        Account to = createAccount(2L, new BigDecimal("500"), AccountStatus.ACTIVE);

        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));
        when(logWriter.logFailure(anyLong(), anyLong(), any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("log failed"));

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100"), "key-1");
        assertThrows(InsufficientBalanceException.class, () -> transferService.transfer(req));
    }

    /* ---------- transferForUser ---------- */

    @Test
    void transferForUser_ShouldSucceed() {
        Account from = createAccount(1L, new BigDecimal("1000"), AccountStatus.ACTIVE);
        Account to = createAccount(2L, new BigDecimal("500"), AccountStatus.ACTIVE);

        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));
        when(logWriter.logSuccess(anyLong(), anyLong(), any(), anyString())).thenReturn(mock(TransactionLog.class));

        TransferResponse result = transferService.transferForUser(1L, 2L, new BigDecimal("200"));
        assertEquals("SUCCESS", result.status());
    }

    /* ---------- doTransferOnce (protected, tested via transfer) ---------- */

    @Test
    void transfer_ShouldThrow_WhenFromAccountNotFound() {
        when(accountRepo.findById(99L)).thenReturn(Optional.empty());

        TransferRequest req = new TransferRequest(99L, 2L, new BigDecimal("100"), "key-1");
        assertThrows(AccountNotFoundException.class, () -> transferService.transfer(req));
    }

    @Test
    void transfer_ShouldThrow_WhenToAccountNotFound() {
        Account from = createAccount(1L, new BigDecimal("1000"), AccountStatus.ACTIVE);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(99L)).thenReturn(Optional.empty());

        TransferRequest req = new TransferRequest(1L, 99L, new BigDecimal("100"), "key-1");
        assertThrows(AccountNotFoundException.class, () -> transferService.transfer(req));
    }

    @Test
    void transfer_ShouldThrow_WhenFromAccountNotActive() {
        Account from = createAccount(1L, new BigDecimal("1000"), AccountStatus.LOCKED);
        Account to = createAccount(2L, new BigDecimal("500"), AccountStatus.ACTIVE);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100"), "key-1");
        assertThrows(AccountNotActiveException.class, () -> transferService.transfer(req));
    }

    @Test
    void transfer_ShouldThrow_WhenToAccountNotActive() {
        Account from = createAccount(1L, new BigDecimal("1000"), AccountStatus.ACTIVE);
        Account to = createAccount(2L, new BigDecimal("500"), AccountStatus.LOCKED);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100"), "key-1");
        assertThrows(AccountNotActiveException.class, () -> transferService.transfer(req));
    }

    @Test
    void transfer_ShouldThrow_WhenAmountIsZeroOrNegative() {
        Account from = createAccount(1L, new BigDecimal("1000"), AccountStatus.ACTIVE);
        Account to = createAccount(2L, new BigDecimal("500"), AccountStatus.ACTIVE);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));

        TransferRequest req = new TransferRequest(1L, 2L, BigDecimal.ZERO, "key-1");
        assertThrows(ValidationException.class, () -> transferService.transfer(req));
    }

    @Test
    void transfer_ShouldThrow_WhenInsufficientBalance() {
        Account from = createAccount(1L, new BigDecimal("50"), AccountStatus.ACTIVE);
        Account to = createAccount(2L, new BigDecimal("500"), AccountStatus.ACTIVE);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(to));

        when(logWriter.logFailure(anyLong(), anyLong(), any(), anyString(), anyString()))
                .thenReturn(mock(TransactionLog.class));

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100"), "key-1");
        assertThrows(InsufficientBalanceException.class, () -> transferService.transfer(req));
    }
}
