
package com.money.draft.service;

import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.repository.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionLogWriterTest {

    @Mock
    private TransactionLogRepository txRepo;

    @InjectMocks
    private TransactionLogWriter logWriter;

    @Test
    void logSuccess_ShouldSaveAndReturn() {
        TransactionLog log = TransactionLog.success(1L, 2L, new BigDecimal("100"), "key-1");
        when(txRepo.save(any())).thenReturn(log);

        TransactionLog result = logWriter.logSuccess(1L, 2L, new BigDecimal("100"), "key-1");
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus().name());
        verify(txRepo).save(any());
    }

    @Test
    void logFailure_ShouldSaveAndReturn() {
        TransactionLog log = TransactionLog.failure(1L, 2L, new BigDecimal("100"), "key-1", "error");
        when(txRepo.save(any())).thenReturn(log);

        TransactionLog result = logWriter.logFailure(1L, 2L, new BigDecimal("100"), "key-1", "error");
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus().name());
        assertEquals("error", result.getFailureReason());
        verify(txRepo).save(any());
    }
}
