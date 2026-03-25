package com.ecommerce.stockservice.client.adapter;

import com.ecommerce.common.exception.ExternalServiceException;
import com.ecommerce.stockservice.base.AbstractBaseIntegrationTest;
import com.ecommerce.stockservice.client.constants.ProductStatus;
import com.ecommerce.stockservice.client.dto.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "application.clients.product.url=http://localhost:${wiremock.server.port}"
})
class ProductClientAdapterIT extends AbstractBaseIntegrationTest {

    @Autowired
    private ProductClientAdapter productClientAdapter;

    @Test
    @DisplayName("Product Service 200 dönerse, JSON doğru şekilde DTO'ya maplenmeli")
    void given_ProductExists_When_ValidateAndGetProduct_Then_ReturnProductResponse() {
        Long productId = 100L;
        Long tenantId = 1L;

        // GIVEN
        stubFor(get(urlEqualTo("/api/v1/products/tenants/" + tenantId + "/" + productId))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "id": 100,
                                    "sku": "IPHONE-15-PRO",
                                    "status": "ACTIVE"
                                }
                                """)));

        // WHEN
        ProductResponse response = productClientAdapter.validateAndGetProduct(productId, tenantId);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.sku()).isEqualTo("IPHONE-15-PRO");
        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("Product Service 404 dönerse, Feign bunu BusinessException'a çevirmeli")
    void given_ProductNotFound_When_ValidateAndGetProduct_Then_ThrowException() {
        Long productId = 999L;
        Long tenantId = 1L;

        stubFor(get(urlEqualTo("/api/v1/products/tenants/" + tenantId + "/" + productId))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"message\": \"Product not found\"}")));

        assertThatThrownBy(() -> productClientAdapter.validateAndGetProduct(productId, tenantId))
                .isInstanceOf(com.ecommerce.common.exception.BusinessException.class)
                .hasMessageContaining("Bu ürün size ait değil veya bulunamadı!");
    }
}