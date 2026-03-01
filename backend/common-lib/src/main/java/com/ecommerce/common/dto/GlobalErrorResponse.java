package com.ecommerce.common.dto;

import java.time.LocalDateTime;

public record GlobalErrorResponse(
        String message,
        String path,
        int statusCode,
        LocalDateTime timestamp
) {
}
