
package com.money.draft.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.money.draft.dto.TransferRequest;
import com.money.draft.dto.TransferResponse;
import com.money.draft.exception.AccountNotFoundException;
import com.money.draft.exception.DuplicateTransferException;
import com.money.draft.exception.InsufficientBalanceException;
import com.money.draft.exception.SelfTransferNotAllowedException;
import com.money.draft.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferController transferController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // If you have a @ControllerAdvice for exceptions, register it here with .setControllerAdvice(...)
        mockMvc = MockMvcBuilders
                .standaloneSetup(transferController)
                //.setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void transfer_ShouldReturnSuccess_WhenTransferIsValid() throws Exception {
        // Given
        TransferRequest request = new TransferRequest(
                100L,
                200L,
                new BigDecimal("150.00"),
                "transfer-key-123"
        );
        TransferResponse expectedResponse = TransferResponse.success(1L, new BigDecimal("150.00"));

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.transactionId", is(1)))
                .andExpect(jsonPath("$.amount", is(150.00)));

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    void transfer_ShouldReturn409_WhenDuplicateIdempotencyKey() throws Exception {
        // Given
        TransferRequest request = new TransferRequest(
                100L,
                200L,
                new BigDecimal("150.00"),
                "duplicate-key"
        );

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new DuplicateTransferException("duplicate-key"));

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    void transfer_ShouldReturn404_WhenAccountNotFound() throws Exception {
        // Given
        TransferRequest request = new TransferRequest(
                999L,
                200L,
                new BigDecimal("150.00"),
                "key-123"
        );

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new AccountNotFoundException(999L)); // <-- Long, not String

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    void transfer_ShouldReturn400_WhenInsufficientBalance() throws Exception {
        // Given
        TransferRequest request = new TransferRequest(
                100L,
                200L,
                new BigDecimal("10000.00"),
                "key-123"
        );

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new InsufficientBalanceException(
                        100L,                             // fromAccountId
                        new BigDecimal("500.00"),         // current balance (example)
                        new BigDecimal("10000.00")        // attempted amount
                ));

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    void transfer_ShouldReturn400_WhenSelfTransfer() throws Exception {
        // Given
        TransferRequest request = new TransferRequest(
                100L,
                100L,
                new BigDecimal("50.00"),
                "key-123"
        );

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new SelfTransferNotAllowedException(100L)); // <-- Long

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    void transfer_ShouldReturn400_WhenAmountIsNegative() throws Exception {
        // Given
        TransferRequest request = new TransferRequest(
                100L,
                200L,
                new BigDecimal("-50.00"),
                "key-123"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transfer(any());
    }

    @Test
    void transfer_ShouldReturn400_WhenFromAccountIdIsNull() throws Exception {
        // Given
        TransferRequest request = new TransferRequest(
                null,
                200L,
                new BigDecimal("50.00"),
                "key-123"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transfer(any());
    }

    @Test
    void transfer_ShouldReturn400_WhenToAccountIdIsNull() throws Exception {
        // Given
        TransferRequest request = new TransferRequest(
                100L,
                null,
                new BigDecimal("50.00"),
                "key-123"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transfer(any());
    }

    @Test
    void transfer_ShouldReturn400_WhenIdempotencyKeyIsBlank() throws Exception {
        // Given
        TransferRequest request = new TransferRequest(
                100L,
                200L,
                new BigDecimal("50.00"),
                ""
        );

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transfer(any());
    }

    @Test
    void transfer_ShouldReturnFailureResponse_WhenServiceReturnsFailure() throws Exception {
        // Given
        TransferRequest request = new TransferRequest(
                100L,
                200L,
                new BigDecimal("50.00"),
                "key-123"
        );
        TransferResponse failureResponse = TransferResponse.failure("Internal error occurred");

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(failureResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Internal error occurred")));

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }
}
