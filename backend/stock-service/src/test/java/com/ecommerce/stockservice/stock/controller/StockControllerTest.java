package com.ecommerce.stockservice.stock.controller;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.common.security.evaluator.TenantSecurityEvaluator;
import com.ecommerce.common.testutils.MockAuthUserTestConfig;
import com.ecommerce.stockservice.client.adapter.ProductClientAdapter;
import com.ecommerce.stockservice.stock.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
@Import({GlobalExceptionHandler.class, MockAuthUserTestConfig.class})
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StockService stockService;

    @MockitoBean
    private ProductClientAdapter productClientAdapter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private TenantSecurityEvaluator tenantSecurity;


    @Test
    @DisplayName("Manuel Stok Ekleme: Geçerli istek 200 OK dönmeli")
    void given_ValidRequest_When_AddManualStock_Then_Return200() throws Exception {
        Long tenantId = 1L;

        when(tenantSecurity.hasRole(anyLong(), anyString())).thenReturn(true);
        when(tenantSecurity.isMember(anyLong())).thenReturn(true);

        Map<String, Object> request = Map.of(
                "warehouseId", 10L,
                "productId", 100L,
                "amount", 50
        );

        mockMvc.perform(post("/api/v1/stocks/tenant/{tenantId}/manual-add", tenantId)
                        .with(csrf())
                        .with(jwt().jwt(builder -> builder.claim("sub", MockAuthUserTestConfig.TEST_USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_OWNER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Yetkisiz Erişim: Token yoksa 401 Unauthorized dönmeli")
    void given_NoToken_When_AddManualStock_Then_Return401() throws Exception {
        mockMvc.perform(post("/api/v1/stocks/tenant/1/manual-add")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Business Exception: Servis hata fırlatırsa Controller uygun hata formatı dönmeli")
    void given_BusinessError_When_AddManualStock_Then_Return400AndErrorMessage() throws Exception {
        when(tenantSecurity.hasRole(anyLong(), anyString())).thenReturn(true);

        doThrow(new com.ecommerce.common.exception.BusinessException("Yetersiz yetki!", "INSUFFICIENT_PERMISSION"))
                .when(stockService).addManualStock(anyLong(), anyLong(), anyLong(), anyInt(), any());

        mockMvc.perform(post("/api/v1/stocks/tenant/1/manual-add")
                        .with(csrf())
                        .with(jwt().jwt(j -> j.claim("sub", MockAuthUserTestConfig.TEST_USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\": 1, \"productId\": 1, \"amount\": 1}"))
                .andExpect(status().isBadRequest());
    }
}