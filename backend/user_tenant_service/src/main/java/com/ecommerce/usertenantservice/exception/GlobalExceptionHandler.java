package com.ecommerce.usertenantservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

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
    public ResponseEntity<GlobalErrorResponse> handlePaymentException(PaymentFailedException exception){

        GlobalErrorResponse errorResponse = new GlobalErrorResponse(
                exception.getMessage(),
                "",
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
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

}
