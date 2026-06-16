package com.ecommerce.productservice.common.exception;

import lombok.Getter;

/**
 * Alt kategorisi veya bağlı ürünü olan bir kategori silinmeye çalışıldığında fırlatılır.
 * CategoryExceptionHandler bunu 409 CONFLICT'e map'ler.
 */
@Getter
public class CategoryInUseException extends RuntimeException {
    private final String errorCode;

    public CategoryInUseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
