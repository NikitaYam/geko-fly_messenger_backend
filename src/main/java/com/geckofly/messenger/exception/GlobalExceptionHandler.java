package com.geckofly.messenger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors.toString()
        ));
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<Map<String, Object>> handleAuthException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Invalid credentials"
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(buildResponse(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "Payload too large",
                "File exceeds the maximum allowed size"
        ));
    }

    // Мл5: битый/непарсируемый JSON в теле запроса — это ошибка клиента (400), не сервера (500).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed request",
                "Request body is missing or not valid JSON"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad request",
                ex.getMessage()
        ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        String reason = ex.getReason() != null ? ex.getReason() : "Error";
        return ResponseEntity.status(ex.getStatusCode()).body(buildResponse(
                ex.getStatusCode().value(),
                reason,
                reason
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {

        log.error("Unhandled exception at request processing", ex);

        // Наружу — обезличенно: детали (SQL, пути, стектрейс) остаются в логе выше (Мл2).
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                "Internal server error"
        ));
    }

    private Map<String, Object> buildResponse(int status, String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status);
        response.put("error", error);
        response.put("message", message);
        return response;
    }
}
