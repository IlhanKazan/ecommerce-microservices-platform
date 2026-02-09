package com.ecommerce.usertenantservice.user.mapper;

import com.ecommerce.usertenantservice.user.controller.dto.request.KeycloakSyncRequest;
import com.ecommerce.usertenantservice.user.controller.dto.request.UserRequest;
import com.ecommerce.usertenantservice.user.controller.dto.response.UserResponse;
import com.ecommerce.usertenantservice.user.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User kcToEntity(KeycloakSyncRequest request);
    UserResponse toResponse(User user);

    // nullValuePropertyMappingStrategy = IGNORE Kaynaktaki null'lari gormezden gelir.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(UserRequest request, @MappingTarget User entity);
}
