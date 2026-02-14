package com.mario.backend.common.exception;

import com.mario.backend.common.dto.ApiResponse;
import com.mario.backend.common.http.HttpClientException;
import com.mario.backend.common.http.NonRetryableHttpException;
import com.mario.backend.common.http.RetryableHttpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        log.error("API Exception: {} - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("Validation error: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("400", message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.error("Bad credentials: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("401", "Invalid email or password"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("401", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("403", "Access denied"));
    }

    @ExceptionHandler(RetryableHttpException.class)
    public ResponseEntity<ApiResponse<Void>> handleRetryableHttpException(RetryableHttpException ex) {
        log.error("Retryable HTTP exception (retries exhausted): url={}, status={}, message={}",
                ex.getUrl(), ex.getHttpStatusCode(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        ErrorCode.EXTERNAL_SERVICE_RETRY_EXHAUSTED.getCode(),
                        ErrorCode.EXTERNAL_SERVICE_RETRY_EXHAUSTED.getMessage()));
    }

    @ExceptionHandler(NonRetryableHttpException.class)
    public ResponseEntity<ApiResponse<Void>> handleNonRetryableHttpException(NonRetryableHttpException ex) {
        log.error("Non-retryable HTTP exception: url={}, status={}, message={}",
                ex.getUrl(), ex.getHttpStatusCode(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(
                        ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE.getCode(),
                        ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE.getMessage()));
    }

    @ExceptionHandler(HttpClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpClientException(HttpClientException ex) {
        log.error("HTTP client exception: url={}, message={}", ex.getUrl(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        ErrorCode.EXTERNAL_SERVICE_RETRY_EXHAUSTED.getCode(),
                        ErrorCode.EXTERNAL_SERVICE_RETRY_EXHAUSTED.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("500", "Internal server error"));
    }
}
