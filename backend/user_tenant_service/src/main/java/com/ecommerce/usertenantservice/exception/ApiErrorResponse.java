package com.ecommerce.usertenantservice.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
        String message,
        String path,
        int statusCode,
        LocalDateTime timestamp,
        String errorCode,
        Map<String, Object> details
) {}
