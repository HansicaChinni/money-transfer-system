package com.money.draft.controller;

import com.money.draft.dto.TransferRequest;
import com.money.draft.dto.TransferResponse;
import com.money.draft.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    // ---------------- SUCCESS CASE ----------------

    @Test
    void transfer_success() throws Exception {
        TransferResponse response =
                TransferResponse.success(99L, BigDecimal.valueOf(100));

        when(transferService.transfer(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "fromAccountId": 1,
                  "toAccountId": 2,
                  "amount": 100,
                  "idempotencyKey": "txn-123"
                }
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value(99))
                .andExpect(jsonPath("$.amount").value(100));
    }


    // ---------------- VALIDATION FAILURE CASE ----------------
    // This assumes @NotNull / @Positive etc. exist on TransferRequest

    @Test
    void transfer_validationFailure() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "fromAccountId": null,
                      "toAccountId": 2,
                      "amount": -10
                    }
                """))
                .andExpect(status().isBadRequest());
    }
}

