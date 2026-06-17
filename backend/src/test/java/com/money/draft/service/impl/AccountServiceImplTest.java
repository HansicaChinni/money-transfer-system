
package com.money.draft.service.impl;

import com.money.draft.domain.audit.AuditLog;
import com.money.draft.domain.audit.AuditLogRepository;
import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.AccountStatus;
import com.money.draft.domain.enums.TransactionStatus;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.AccountResponse;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.exception.AccountNotFoundException;
import com.money.draft.exception.IncorrectPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepo;
    @Mock
    private TransactionLogRepository txRepo;
    @Mock
    private AppUserRepository userRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogRepository auditLogRepo;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account createAccount(Long id, BigDecimal balance, AccountStatus status) {
        Account a = new Account();
        a.setId(id);
        a.setAccountNumber("ACC-" + id);
        a.setHolderName("Holder");
        a.setBalance(balance);
        a.setStatus(status);
        return a;
    }

    /* ---------- getAccount ---------- */

    @Test
    void getAccount_ShouldReturn_WhenFound() {
        Account a = createAccount(1L, new BigDecimal("1000"), AccountStatus.ACTIVE);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(a));

        AccountResponse result = accountService.getAccount(1L);
        assertEquals(1L, result.id());
        assertEquals("1000", result.balance().toString());
        assertEquals("ACTIVE", result.status());
    }

    @Test
    void getAccount_ShouldThrow_WhenNotFound() {
        when(accountRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(99L));
    }

    /* ---------- getBalance ---------- */

    @Test
    void getBalance_ShouldReturnBalance() {
        Account a = createAccount(1L, new BigDecimal("500"), AccountStatus.ACTIVE);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(a));

        BigDecimal result = accountService.getBalance(1L);
        assertEquals(new BigDecimal("500"), result);
    }

    @Test
    void getBalance_ShouldThrow_WhenNotFound() {
        when(accountRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> accountService.getBalance(99L));
    }

    /* ---------- getTransactions ---------- */

    @Test
    void getTransactions_ShouldReturnAllRelated() {
        TransactionLog tx = TransactionLog.success(1L, 2L, new BigDecimal("100"), "key-1");
        when(txRepo.findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(1L, 1L)).thenReturn(List.of(tx));

        List<TransactionLogResponse> result = accountService.getTransactions(1L);
        assertEquals(1, result.size());
        assertEquals("SUCCESS", result.get(0).status());
    }

    @Test
    void getTransactions_ShouldReturnEmpty_WhenNone() {
        when(txRepo.findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(1L, 1L)).thenReturn(List.of());
        assertTrue(accountService.getTransactions(1L).isEmpty());
    }

    /* ---------- changePassword ---------- */

    @Test
    void changePassword_ShouldUpdate_WhenCurrentPasswordCorrect() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("john");
        user.setPassword("encoded-old");
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encoded-new");

        accountService.changePassword(1L, "oldPass", "newPass");

        verify(userRepo).save(user);
        verify(auditLogRepo).save(any(AuditLog.class));
        assertEquals("encoded-new", user.getPassword());
    }

    @Test
    void changePassword_ShouldThrow_WhenCurrentPasswordIncorrect() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setPassword("encoded-old");
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        assertThrows(IncorrectPasswordException.class, () -> accountService.changePassword(1L, "wrong", "newPass"));
    }

    @Test
    void changePassword_ShouldThrow_WhenUserNotFound() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> accountService.changePassword(99L, "p", "n"));
    }
}
