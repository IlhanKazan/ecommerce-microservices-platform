package com.ecommerce.stockservice.common.exception;

import lombok.Getter;

/**
 * Depoda hâlâ stok kaydı varken kalıcı silme denendiğinde fırlatılır.
 * StockExceptionHandler bunu 409 CONFLICT'e map'ler.
 */
@Getter
public class WarehouseNotEmptyException extends RuntimeException {
    private final String errorCode;

    public WarehouseNotEmptyException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
