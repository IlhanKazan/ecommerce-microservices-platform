package com.ecommerce.stockservice.common.exception;

import com.ecommerce.common.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class StockExceptionHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
            OptimisticLockingFailureException ex, WebRequest request) {
        log.warn("Stok eş zamanlı güncelleme çakışması: {}", ex.getMessage());
        ApiErrorResponse body = new ApiErrorResponse(
                "Stok eş zamanlı güncellemede çakışma oluştu. Lütfen tekrar deneyin.",
                request.getDescription(false).replace("uri=", ""),
                HttpStatus.CONFLICT.value(),
                LocalDateTime.now(),
                "CONCURRENT_MODIFICATION",
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handlePessimisticLock(
            PessimisticLockingFailureException ex, WebRequest request) {
        log.warn("Stok rezervasyonu lock timeout: {}", ex.getMessage());
        ApiErrorResponse body = new ApiErrorResponse(
                "Stok rezervasyonu zaman aşımına uğradı. Lütfen tekrar deneyin.",
                request.getDescription(false).replace("uri=", ""),
                HttpStatus.CONFLICT.value(),
                LocalDateTime.now(),
                "STOCK_LOCK_TIMEOUT",
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(WarehouseNotEmptyException.class)
    public ResponseEntity<ApiErrorResponse> handleWarehouseNotEmpty(
            WarehouseNotEmptyException ex, WebRequest request) {
        log.warn("Stoklu depo silme reddedildi: {}", ex.getMessage());
        ApiErrorResponse body = new ApiErrorResponse(
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                HttpStatus.CONFLICT.value(),
                LocalDateTime.now(),
                ex.getErrorCode(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
