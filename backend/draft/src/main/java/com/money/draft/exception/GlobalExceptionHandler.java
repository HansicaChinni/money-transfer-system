
package com.money.draft.exception;

import com.money.draft.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Bean Validation errors from @Valid DTOs ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .toList();
        ErrorResponse body = new ErrorResponse("VALIDATION_FAILED", "One or more fields are invalid", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // --- Domain/business exceptions ---
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AccountNotFoundException ex) {
        return toResponse(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleNotActive(AccountNotActiveException ex) {
        return toResponse(HttpStatus.CONFLICT, "ACCOUNT_NOT_ACTIVE", ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficient(InsufficientBalanceException ex) {
        return toResponse(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", ex.getMessage());
    }

    @ExceptionHandler(DuplicateTransferException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateTransferException ex) {
        return toResponse(HttpStatus.CONFLICT, "DUPLICATE_TRANSFER", ex.getMessage());
    }

    @ExceptionHandler(SelfTransferNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleSelf(SelfTransferNotAllowedException ex) {
        return toResponse(HttpStatus.BAD_REQUEST, "SELF_TRANSFER_NOT_ALLOWED", ex.getMessage());
    }

    // --- Catch-all fallback (optional) ---
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return toResponse(HttpStatus.BAD_REQUEST, "BUSINESS_ERROR", ex.getMessage());
    }

    private static ResponseEntity<ErrorResponse> toResponse(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, null));
    }

    private static String formatFieldError(FieldError fe) {
        return "%s: %s".formatted(fe.getField(), fe.getDefaultMessage());
    }
}
