package com.sudesh.ledger.shared.error;

import com.sudesh.ledger.command.domain.exception.AccountNotFoundException;
import com.sudesh.ledger.command.domain.exception.InsufficientFundsException;
import com.sudesh.ledger.shared.exception.ConcurrencyException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bean Validation failures (@Valid on request DTOs) → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(ApiError.of(
                400, "Bad Request", "Validation failed", req.getRequestURI(), details));
    }

    // Business rule violation — amount ≤ 0, account not open, etc → 422
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.unprocessableEntity().body(ApiError.of(
                422, "Unprocessable Entity", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return ResponseEntity.unprocessableEntity().body(ApiError.of(
                422, "Unprocessable Entity", ex.getMessage(), req.getRequestURI()));
    }

    // Insufficient funds — a specific, expected business rejection → 422
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest req) {
        return ResponseEntity.unprocessableEntity().body(ApiError.of(
                422, "Insufficient Funds", ex.getMessage(), req.getRequestURI()));
    }

    // Account doesn't exist → 404
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AccountNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
                404, "Not Found", ex.getMessage(), req.getRequestURI()));
    }

    // Optimistic concurrency conflict — someone else wrote to this aggregate first → 409
    @ExceptionHandler(ConcurrencyException.class)
    public ResponseEntity<ApiError> handleConcurrency(ConcurrencyException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                409, "Conflict", ex.getMessage(), req.getRequestURI()));
    }

    // Catch-all — never leak a raw stack trace to the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        return ResponseEntity.internalServerError().body(ApiError.of(
                500, "Internal Server Error", "An unexpected error occurred", req.getRequestURI()));
    }
}