package com.ecommerce.usertenantservice.user.controller.dto.request;

import com.ecommerce.usertenantservice.tenant.entity.UserTenant;
import com.ecommerce.usertenantservice.user.entity.Address;

import java.util.Set;

public record UserRequest(
        String phoneNumber,
        String profileImageUrl,
        String language,
        Set<UserTenant> memberships,
        Set<Address> addresses
) {
}
