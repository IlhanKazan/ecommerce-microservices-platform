package com.ecommerce.stockservice.stock.controller;

import com.ecommerce.common.aop.IdempotencyAspect;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.common.security.evaluator.TenantSecurityEvaluator;
import com.ecommerce.common.testutils.MockAuthUserTestConfig;
import com.ecommerce.stockservice.client.adapter.ProductClientAdapter;
import com.ecommerce.stockservice.stock.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
@EnableAspectJAutoProxy
@Import({
        IdempotencyAspect.class,
        com.ecommerce.common.exception.GlobalExceptionHandler.class,
        com.ecommerce.common.testutils.MockAuthUserTestConfig.class
})
class StockControllerIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StockService stockService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private TenantSecurityEvaluator tenantSecurity;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("Idempotency: Aynı Key ile İLK defa gelindiğinde istek 200 OK dönmeli")
    void given_FirstRequest_When_AddManualStock_Then_Return200() throws Exception {
        String idempotencyKey = "test-req-12345";

        when(tenantSecurity.hasRole(anyLong(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        Map<String, Object> requestBody = Map.of("warehouseId", 10L, "productId", 100L, "amount", 50);

        mockMvc.perform(post("/api/v1/stocks/tenant/1/manual-add")
                        .with(csrf())
                        .header("Idempotency-Key", idempotencyKey)
                        .with(jwt().jwt(j -> j.claim("sub", MockAuthUserTestConfig.TEST_USER_ID.toString())).authorities(new SimpleGrantedAuthority("ROLE_OWNER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Idempotency: Aynı Key ile İKİNCİ defa (çift tıklama) gelindiğinde 400 Bad Request dönmeli")
    void given_DuplicateRequest_When_AddManualStock_Then_Return400() throws Exception {
        String idempotencyKey = "test-req-12345";

        when(tenantSecurity.hasRole(anyLong(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);

        Map<String, Object> requestBody = Map.of("warehouseId", 10L, "productId", 100L, "amount", 50);

        mockMvc.perform(post("/api/v1/stocks/tenant/1/manual-add")
                        .with(csrf())
                        .header("Idempotency-Key", idempotencyKey)
                        .with(jwt().jwt(j -> j.claim("sub", MockAuthUserTestConfig.TEST_USER_ID.toString())).authorities(new SimpleGrantedAuthority("ROLE_OWNER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Idempotency: Header eksikse direkt 400 Bad Request dönmeli")
    void given_NoHeader_When_AddManualStock_Then_Return400() throws Exception {
        when(tenantSecurity.hasRole(anyLong(), anyString())).thenReturn(true);

        Map<String, Object> requestBody = Map.of("warehouseId", 10L, "productId", 100L, "amount", 50);

        mockMvc.perform(post("/api/v1/stocks/tenant/1/manual-add")
                        .with(csrf())
                        .with(jwt().jwt(j -> j.claim("sub", MockAuthUserTestConfig.TEST_USER_ID.toString())).authorities(new SimpleGrantedAuthority("ROLE_OWNER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }
}