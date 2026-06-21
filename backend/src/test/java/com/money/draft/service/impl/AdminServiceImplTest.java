
package com.money.draft.service.impl;

import com.money.draft.domain.audit.AuditLog;
import com.money.draft.domain.audit.AuditLogRepository;
import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.AccountStatus;
import com.money.draft.domain.enums.Role;
import com.money.draft.domain.enums.TransactionStatus;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.AdminAccountDetailResponse;
import com.money.draft.dto.AdminAccountView;
import com.money.draft.dto.AdminCreateAccountRequest;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.exception.AccountClosureException;
import com.money.draft.exception.AccountNotFoundException;
import com.money.draft.exception.BusinessException;
import com.money.draft.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

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
    private AdminServiceImpl adminService;

    /* ---------- getAllAccounts ---------- */

    @Test
    void getAllAccounts_ShouldReturnAllAccounts() {
        Account a1 = createAccount(1L, "ACC-1", new BigDecimal("1000"), AccountStatus.ACTIVE);
        Account a2 = createAccount(2L, "ACC-2", new BigDecimal("500"), AccountStatus.LOCKED);
        when(accountRepo.findAll()).thenReturn(List.of(a1, a2));

        List<AdminAccountView> result = adminService.getAllAccounts();
        assertEquals(2, result.size());
        assertEquals("ACTIVE", result.get(0).status());
        assertEquals("LOCKED", result.get(1).status());
    }

    @Test
    void getAllAccounts_ShouldReturnEmpty_WhenNone() {
        when(accountRepo.findAll()).thenReturn(List.of());
        assertTrue(adminService.getAllAccounts().isEmpty());
    }

    /* ---------- getAllTransactions ---------- */

    @Test
    void getAllTransactions_ShouldReturnAll() {
        TransactionLog tx = TransactionLog.success(1L, 2L, new BigDecimal("100"), "key-1");
        when(txRepo.findAllByOrderByCreatedOnDesc()).thenReturn(List.of(tx));

        List<TransactionLogResponse> result = adminService.getAllTransactions();
        assertEquals(1, result.size());
        assertEquals("SUCCESS", result.get(0).status());
    }

    @Test
    void getAllTransactions_ShouldReturnEmpty_WhenNone() {
        when(txRepo.findAllByOrderByCreatedOnDesc()).thenReturn(List.of());
        assertTrue(adminService.getAllTransactions().isEmpty());
    }

    /* ---------- createAccount ---------- */

    @Test
    void createAccount_ShouldCreateAccountAndUser() {
        var req = new AdminCreateAccountRequest("newuser", "pass123", "New User", new BigDecimal("5000"));
        when(userRepo.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");

        Account savedAccount = new Account();
        savedAccount.setId(1L);
        savedAccount.setHolderName("New User");
        savedAccount.setBalance(new BigDecimal("5000"));
        savedAccount.setStatus(AccountStatus.ACTIVE);

        when(accountRepo.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            if (a.getId() == null) a.setId(1L);
            return a;
        });

        AdminAccountDetailResponse result = adminService.createAccount(req);
        assertEquals("New User", result.holderName());
        assertEquals("ACTIVE", result.status());
        verify(accountRepo, times(2)).save(any(Account.class));
        verify(userRepo).save(any(AppUser.class));
    }

    @Test
    void createAccount_ShouldThrow_WhenUsernameExists() {
        var req = new AdminCreateAccountRequest("existing", "pass", "User", new BigDecimal("1000"));
        when(userRepo.findByUsername("existing")).thenReturn(Optional.of(new AppUser()));
        assertThrows(BusinessException.class, () -> adminService.createAccount(req));
    }

    /* ---------- getAccountDetails ---------- */

    @Test
    void getAccountDetails_ShouldReturnDetails() {
        Account a = createAccount(1L, "ACC-1", new BigDecimal("500"), AccountStatus.ACTIVE);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(a));

        AdminAccountDetailResponse result = adminService.getAccountDetails(1L);
        assertEquals(1L, result.id());
        assertEquals("ACC-1", result.accountNumber());
    }

    @Test
    void getAccountDetails_ShouldThrow_WhenNotFound() {
        when(accountRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> adminService.getAccountDetails(99L));
    }

    /* ---------- updateAccountStatus ---------- */

    @Test
    void updateAccountStatus_ShouldChangeStatus() {
        Account a = createAccount(1L, "ACC-1", BigDecimal.ZERO, AccountStatus.ACTIVE);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(a));
        when(accountRepo.save(any())).thenReturn(a);

        AdminAccountDetailResponse result = adminService.updateAccountStatus(1L, AccountStatus.LOCKED);
        assertEquals("LOCKED", result.status());
        verify(auditLogRepo).save(any(AuditLog.class));
    }

    @Test
    void updateAccountStatus_ShouldThrow_WhenClosingWithNonZeroBalance() {
        Account a = createAccount(1L, "ACC-1", new BigDecimal("500"), AccountStatus.ACTIVE);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(a));
        assertThrows(AccountClosureException.class, () -> adminService.updateAccountStatus(1L, AccountStatus.CLOSED));
    }

    @Test
    void updateAccountStatus_ShouldClose_WhenZeroBalance() {
        Account a = createAccount(1L, "ACC-1", BigDecimal.ZERO, AccountStatus.ACTIVE);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(a));
        when(accountRepo.save(any())).thenReturn(a);

        AdminAccountDetailResponse result = adminService.updateAccountStatus(1L, AccountStatus.CLOSED);
        assertEquals("CLOSED", result.status());
        verify(auditLogRepo).save(any(AuditLog.class));
    }

    @Test
    void updateAccountStatus_ShouldThrow_WhenAccountNotFound() {
        when(accountRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> adminService.updateAccountStatus(99L, AccountStatus.ACTIVE));
    }

    /* ---------- updateDailyLimit ---------- */

    @Test
    void updateDailyLimit_ShouldUpdate() {
        Account a = createAccount(1L, "ACC-1", BigDecimal.ZERO, AccountStatus.ACTIVE);
        a.setDailyTransferLimit(new BigDecimal("50000"));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(a));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdminAccountDetailResponse result = adminService.updateDailyLimit(1L, new BigDecimal("75000"));
        assertEquals("75000", result.dailyTransferLimit().toString());
        verify(auditLogRepo).save(any(AuditLog.class));
    }

    @Test
    void updateDailyLimit_ShouldThrow_WhenNull() {
        assertThrows(ValidationException.class, () -> adminService.updateDailyLimit(1L, null));
    }

    @Test
    void updateDailyLimit_ShouldThrow_WhenNegative() {
        assertThrows(ValidationException.class, () -> adminService.updateDailyLimit(1L, new BigDecimal("-100")));
    }

    @Test
    void updateDailyLimit_ShouldThrow_WhenZero() {
        assertThrows(ValidationException.class, () -> adminService.updateDailyLimit(1L, BigDecimal.ZERO));
    }

    @Test
    void updateDailyLimit_ShouldThrow_WhenAccountNotFound() {
        when(accountRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> adminService.updateDailyLimit(99L, new BigDecimal("10000")));
    }

    private Account createAccount(Long id, String accNum, BigDecimal balance, AccountStatus status) {
        Account a = new Account();
        a.setId(id);
        a.setAccountNumber(accNum);
        a.setHolderName("Holder");
        a.setBalance(balance);
        a.setStatus(status);
        return a;
    }
}
