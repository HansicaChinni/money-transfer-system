
package com.money.draft.exception;

import com.money.draft.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Bean Validation errors from @Valid DTOs ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        // Take the first field error for a concise message. Optionally accumulate all.
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("Validation failed");
        return new ErrorResponse("VALIDATION_ERROR", message, req.getRequestURI(), Instant.now());
    }

    // --- Domain/business exceptions (specific) ---
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

    // --- Catch-all for other BusinessException subtypes ---
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBusiness(BusinessException ex, HttpServletRequest req) {
        return toError(ex.getCode() != null ? ex.getCode() : "BUSINESS_ERROR", ex.getMessage(), req);
    }

    private ErrorResponse toError(String code, String message, HttpServletRequest req) {
        return new ErrorResponse(code, message, req.getRequestURI(), Instant.now());
    }
}
