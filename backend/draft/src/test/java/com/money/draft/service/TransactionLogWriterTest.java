package com.money.draft.service;

import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.repository.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionLogWriterTest {

    @Mock
    private TransactionLogRepository txRepo;

    @InjectMocks
    private TransactionLogWriter writer;

    @Test
    void logSuccess_savesTransaction() {
        when(txRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionLog tx =
                writer.logSuccess(1L, 2L, BigDecimal.TEN, "key");

        assertNotNull(tx);
    }

    @Test
    void logFailure_savesTransaction() {
        when(txRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionLog tx =
                writer.logFailure(1L, 2L, BigDecimal.TEN, "key", "reason");

        assertNotNull(tx);
    }
}
