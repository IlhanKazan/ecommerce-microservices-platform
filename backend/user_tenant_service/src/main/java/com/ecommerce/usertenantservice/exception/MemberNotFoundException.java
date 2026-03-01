package com.ecommerce.usertenantservice.exception;

import com.ecommerce.common.exception.ResourceNotFoundException;

public class MemberNotFoundException extends ResourceNotFoundException {
    public MemberNotFoundException(String message) {
        super(message, "MEMBER_NOT_FOUND");
    }
}
