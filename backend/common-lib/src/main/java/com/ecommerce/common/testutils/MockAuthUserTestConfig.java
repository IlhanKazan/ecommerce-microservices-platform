package com.ecommerce.common.testutils;

import com.ecommerce.common.security.dto.AuthUser;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.UUID;

/**
 * Bu sınıf sadece Controller testlerinde,
 * @CurrentUser AuthUser user parametresine mock kullanıcı basmak için kullanılır.
 */
@TestConfiguration
public class MockAuthUserTestConfig implements WebMvcConfigurer {

    public static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(AuthUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new AuthUser(TEST_USER_ID, "mock_user", "test@ecommerce.com", "Mock", "Person");
            }
        });
    }
}