package com.ecommerce.usertenantservice.user.exception;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(UsernameNotFoundException.class)
    public String userNotFoundException(UsernameNotFoundException e) {
        return "[" + e.getMessage() + "] user not found!";
    }

}
