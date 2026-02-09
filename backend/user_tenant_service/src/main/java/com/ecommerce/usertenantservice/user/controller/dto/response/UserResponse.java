package com.ecommerce.usertenantservice.user.controller.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
   String username,
   String email,
   String firstName,
   String lastName,
   String phoneNumber,
   String profileImageUrl,
   LocalDateTime createdAt,
   LocalDateTime updatedAt,
   boolean isMerchant
) {
    // Sadece isMerchant field'ını değiştiren bir metod
    public UserResponse withMerchantStatus(boolean newMerchantStatus) {
        return new UserResponse(
                username,
                email,
                firstName,
                lastName,
                phoneNumber,
                profileImageUrl,
                createdAt,
                updatedAt,
                newMerchantStatus
        );
    }
}
