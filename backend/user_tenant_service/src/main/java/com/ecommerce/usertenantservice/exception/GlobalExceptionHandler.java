package com.ecommerce.usertenantservice.exception;

import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalErrorResponse> handleGlobalException(Exception exception, WebRequest request){

        GlobalErrorResponse errorResponse = new GlobalErrorResponse(
                exception.getMessage(),
                request.getDescription(false),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<GlobalErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception, WebRequest request) {

        GlobalErrorResponse errorResponse = new GlobalErrorResponse(
                exception.getMessage(),
                request.getDescription(false),
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentFailedException(PaymentFailedException exception){

        Map<String, Object> details = new HashMap<>();
        details.put("tenantId", exception.getTenantId());
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                exception.getMessage(),
                "",
                HttpStatus.PAYMENT_REQUIRED.value(),
                LocalDateTime.now(),
                TenantStatus.PAYMENT_FAILED.name(),
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception, WebRequest request) {
        Map<String, Object> errors = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ApiErrorResponse response = new ApiErrorResponse(
                "Lütfen girdiğiniz bilgileri kontrol edin.",
                request.getDescription(false),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                "VALIDATION_ERROR",
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<GlobalErrorResponse> handleMemberNotFoundException(MemberNotFoundException exception, WebRequest request){

        GlobalErrorResponse errorResponse = new GlobalErrorResponse(
                exception.getMessage(),
                request.getDescription(false),
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OwnerTenantException.class)
    public ResponseEntity<GlobalErrorResponse> handleOwnerTenantException(OwnerTenantException exception, WebRequest request){

        GlobalErrorResponse errorResponse = new GlobalErrorResponse(
                exception.getMessage(),
                request.getDescription(false),
                HttpStatus.NOT_ACCEPTABLE.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(TenantCreationException.class)
    public ResponseEntity<GlobalErrorResponse> handleTenantCreationException(TenantCreationException exception, WebRequest request){

        GlobalErrorResponse errorResponse = new GlobalErrorResponse(
                exception.getMessage(),
                request.getDescription(false),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(PaymentServiceUnreachableException.class)
    public ResponseEntity<GlobalErrorResponse> handlePaymentServiceUnreachableException(PaymentServiceUnreachableException exception, WebRequest request){

        GlobalErrorResponse errorResponse = new GlobalErrorResponse(
                exception.getMessage(),
                request.getDescription(false),
                HttpStatus.FORBIDDEN.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UnexpectedErrorException.class)
    public ResponseEntity<GlobalErrorResponse> handleUnexpectedErrorException(UnexpectedErrorException exception, WebRequest request){

        GlobalErrorResponse errorResponse = new GlobalErrorResponse(
                exception.getMessage(),
                request.getDescription(false),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
