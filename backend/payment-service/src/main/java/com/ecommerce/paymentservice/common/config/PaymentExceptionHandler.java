package com.ecommerce.paymentservice.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * common-lib GlobalExceptionHandler exception loglamıyor.
 * Bu handler daha yüksek öncelikte çalışır ve her exception'ı loglar.
 */
@RestControllerAdvice
@Order(-1)
@Slf4j
public class PaymentExceptionHandler {

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
