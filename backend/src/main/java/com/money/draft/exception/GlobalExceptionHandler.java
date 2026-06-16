package com.money.draft.exception;

import com.money.draft.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("Validation failed");
        return new ErrorResponse("VALIDATION_ERROR", message, req.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(AccountNotFoundException ex, HttpServletRequest req) {
        return toError(ex.getCode(), ex.getMessage(), req);
    }

    @ExceptionHandler(AccountNotActiveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleNotActive(AccountNotActiveException ex, HttpServletRequest req) {
        return toError(ex.getCode(), ex.getMessage(), req);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficient(InsufficientBalanceException ex, HttpServletRequest req) {
        return toError(ex.getCode(), ex.getMessage(), req);
    }

    @ExceptionHandler(DailyLimitExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleDailyLimit(DailyLimitExceededException ex, HttpServletRequest req) {
        return toError(ex.getCode(), ex.getMessage(), req);
    }

    @ExceptionHandler(AccountClosureException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleClosure(AccountClosureException ex, HttpServletRequest req) {
        return toError(ex.getCode(), ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateTransferException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DuplicateTransferException ex, HttpServletRequest req) {
        return toError(ex.getCode(), ex.getMessage(), req);
    }

    @ExceptionHandler(SelfTransferNotAllowedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleSelf(SelfTransferNotAllowedException ex, HttpServletRequest req) {
        return toError(ex.getCode(), ex.getMessage(), req);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBusiness(BusinessException ex, HttpServletRequest req) {
        return toError(ex.getCode() != null ? ex.getCode() : "BUSINESS_ERROR", ex.getMessage(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMalformed(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return new ErrorResponse("MALFORMED_REQUEST", "Request body is malformed or invalid", req.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return new ErrorResponse("METHOD_NOT_ALLOWED", ex.getMessage(), req.getRequestURI(), Instant.now());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return new ErrorResponse("MISSING_PARAM", ex.getMessage(), req.getRequestURI(), Instant.now());
    }

    private ErrorResponse toError(String code, String message, HttpServletRequest req) {
        return new ErrorResponse(code, message, req.getRequestURI(), Instant.now());
    }
}
