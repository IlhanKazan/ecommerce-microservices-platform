package com.ecommerce.productservice.common.exception;

import com.ecommerce.common.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CategoryExceptionHandler {

    @ExceptionHandler(CategoryInUseException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryInUse(
            CategoryInUseException ex, WebRequest request) {
        log.warn("Dolu kategori silme reddedildi: {}", ex.getMessage());
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
