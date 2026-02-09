package com.ecommerce.usertenantservice.exception;

import java.time.LocalDateTime;

public record GlobalErrorResponse(
        String message,
        String path,
        int statusCode,
        LocalDateTime timestamp
) {
}
