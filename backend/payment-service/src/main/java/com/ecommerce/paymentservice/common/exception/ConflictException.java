package com.ecommerce.paymentservice.common.exception;

/**
 * İş kuralı çakışması — HTTP 409. (Örn: aktif aboneliğin tek kartını silme,
 * varsayılan kart olmadan upgrade, aktif abonelik yokken plan değiştirme.)
 * common-lib GlobalExceptionHandler 409 mapping'i sunmadığı için lokal tutulur;
 * PaymentExceptionHandler 409'a map'ler.
 */
public class ConflictException extends RuntimeException {
    private final String errorCode;

    public ConflictException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
