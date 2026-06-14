package com.rishi.finledger.exception;

import com.rishi.finledger.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // USER NOT FOUND
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {

        log.warn("User not found at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(ex, request, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    // WALLET NOT FOUND
    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFound(
            WalletNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Wallet not found at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(ex, request, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    // INSUFFICIENT BALANCE
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleBalance(
            InsufficientBalanceException ex,
            HttpServletRequest request) {

        log.warn("Balance error at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(ex, request, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    // INVALID TRANSACTION
    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransaction(
            InvalidTransactionException ex,
            HttpServletRequest request) {

        log.warn("Invalid transaction at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(ex, request, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    // EMAIL ALREADY EXISTS
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {

        log.warn("Duplicate email at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(ex, request, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    // INVALID CREDENTIALS
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        log.warn("Login failed at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(ex, request, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    // VALIDATION ERROR
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");

        log.warn("Validation failed at {}: {}", request.getRequestURI(), errorMessage);

        return new ResponseEntity<>(
                new ErrorResponse(
                        LocalDateTime.now(),
                        400,
                        "BAD_REQUEST",
                        errorMessage,
                        request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    // DATABASE ERROR (IDEMPOTENCY / UNIQUE)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("DB constraint violation at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(ex, request, HttpStatus.CONFLICT, "CONFLICT");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(
            RateLimitExceededException ex,
            HttpServletRequest request) {

        log.warn("Rate limit exceeded at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(ex, request, HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS");
    }

    // Authorization Denied Exception
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AuthorizationDeniedException ex,
            HttpServletRequest request) {

        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(ex, request, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDenied(
            org.springframework.security.authorization.AuthorizationDeniedException ex,
            HttpServletRequest request) {

        return buildResponse(ex, request, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    // GENERIC FALLBACK
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    // COMMON BUILDER
    private ResponseEntity<ErrorResponse> buildResponse(
            Exception ex,
            HttpServletRequest request,
            HttpStatus status,
            String errorCode) {

        return new ResponseEntity<>(
                new ErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        errorCode,
                        ex.getMessage(),
                        request.getRequestURI()),
                status);
    }
}