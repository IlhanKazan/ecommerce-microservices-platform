package com.ecommerce.common.dto;

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
