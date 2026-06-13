package com.ecommerce.paymentservice.common.config;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ExternalServiceException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.paymentservice.common.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * common-lib GlobalExceptionHandler exception loglamıyor.
 * Bu handler daha yüksek öncelikte (@Order -1) çalışır ve her exception'ı loglar.
 * Order(-1) tüm exception'ları bu advice'a yönlendirdiği için (Exception.class catch-all),
 * 404/400/409 statülerini de burada açıkça map'lemek zorundayız; aksi halde hepsi 500 olur.
 */
@RestControllerAdvice
@Order(-1)
@Slf4j
public class PaymentExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Payment service not found — path: {}, code: {}, message: {}",
                request.getDescription(false), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "message", ex.getMessage() != null ? ex.getMessage() : "Not found",
                "errorCode", ex.getErrorCode(),
                "path", request.getDescription(false),
                "statusCode", 404,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex, WebRequest request) {
        log.warn("Payment service business error — path: {}, code: {}, message: {}",
                request.getDescription(false), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", ex.getMessage() != null ? ex.getMessage() : "Bad request",
                "errorCode", ex.getErrorCode(),
                "path", request.getDescription(false),
                "statusCode", 400,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Payment service access denied — path: {}, message: {}",
                request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "message", ex.getMessage() != null ? ex.getMessage() : "Forbidden",
                "errorCode", "ACCESS_DENIED",
                "path", request.getDescription(false),
                "statusCode", 403,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Map<String, Object>> handleExternal(ExternalServiceException ex, WebRequest request) {
        log.error("Payment service external error — path: {}, code: {}, message: {}",
                request.getDescription(false), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "message", ex.getMessage() != null ? ex.getMessage() : "External service error",
                "errorCode", ex.getErrorCode(),
                "path", request.getDescription(false),
                "statusCode", 503,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex, WebRequest request) {
        log.warn("Payment service conflict — path: {}, code: {}, message: {}",
                request.getDescription(false), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", ex.getMessage() != null ? ex.getMessage() : "Conflict",
                "errorCode", ex.getErrorCode(),
                "path", request.getDescription(false),
                "statusCode", 409,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex, WebRequest request) {
        log.error("Payment service unhandled exception — path: {}, message: {}",
                request.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", ex.getMessage() != null ? ex.getMessage() : "Internal error",
                "path", request.getDescription(false),
                "statusCode", 500,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
