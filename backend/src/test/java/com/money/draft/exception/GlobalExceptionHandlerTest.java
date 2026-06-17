
package com.money.draft.exception;

import com.money.draft.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.math.BigDecimal;
import java.util.Collections;

import org.springframework.http.HttpInputMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/test");
    }

    @Test
    void handleAccountNotFoundException() {
        AccountNotFoundException ex = new AccountNotFoundException(99L);
        ErrorResponse resp = handler.handleNotFound(ex, request);
        assertEquals("ACCOUNT_NOT_FOUND", resp.code());
        assertEquals("/test", resp.path());
    }

    @Test
    void handleAccountNotActiveException() {
        AccountNotActiveException ex = new AccountNotActiveException(1L, "LOCKED");
        ErrorResponse resp = handler.handleNotActive(ex, request);
        assertEquals("ACCOUNT_NOT_ACTIVE", resp.code());
    }

    @Test
    void handleInsufficientBalanceException() {
        InsufficientBalanceException ex = new InsufficientBalanceException(1L, new BigDecimal("100"), new BigDecimal("500"));
        ErrorResponse resp = handler.handleInsufficient(ex, request);
        assertEquals("INSUFFICIENT_FUNDS", resp.code());
    }

    @Test
    void handleDailyLimitExceededException() {
        DailyLimitExceededException ex = new DailyLimitExceededException(1L, new BigDecimal("100000"), new BigDecimal("150000"));
        ErrorResponse resp = handler.handleDailyLimit(ex, request);
        assertEquals("DAILY_LIMIT_EXCEEDED", resp.code());
    }

    @Test
    void handleAccountClosureException() {
        AccountClosureException ex = new AccountClosureException(1L, new BigDecimal("500"));
        ErrorResponse resp = handler.handleClosure(ex, request);
        assertEquals("ACCOUNT_NOT_CLOSED", resp.code());
    }

    @Test
    void handleDuplicateTransferException() {
        DuplicateTransferException ex = new DuplicateTransferException("key-1");
        ErrorResponse resp = handler.handleDuplicate(ex, request);
        assertEquals("DUPLICATE_TRANSFER", resp.code());
    }

    @Test
    void handleSelfTransferNotAllowedException() {
        SelfTransferNotAllowedException ex = new SelfTransferNotAllowedException(100L);
        ErrorResponse resp = handler.handleSelf(ex, request);
        assertEquals("SELF_TRANSFER_NOT_ALLOWED", resp.code());
    }

    @Test
    void handleBusinessException() {
        BusinessException ex = new BusinessException("CUSTOM_CODE", "custom error");
        ErrorResponse resp = handler.handleBusiness(ex, request);
        assertEquals("CUSTOM_CODE", resp.code());
    }

    @Test
    void handleBusinessException_WithNullCode() {
        BusinessException ex = new BusinessException(null, "error");
        ErrorResponse resp = handler.handleBusiness(ex, request);
        assertEquals("BUSINESS_ERROR", resp.code());
    }

    @Test
    void handleHttpMessageNotReadableException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("malformed", mock(HttpInputMessage.class));
        ErrorResponse resp = handler.handleMalformed(ex, request);
        assertEquals("MALFORMED_REQUEST", resp.code());
    }

    @Test
    void handleHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST", Collections.singletonList("GET"));
        ErrorResponse resp = handler.handleMethodNotAllowed(ex, request);
        assertEquals("METHOD_NOT_ALLOWED", resp.code());
    }

    @Test
    void handleMissingServletRequestParameterException() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("param", "String");
        ErrorResponse resp = handler.handleMissingParam(ex, request);
        assertEquals("MISSING_PARAM", resp.code());
    }
}
