package com.ecommerce.productservice.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.productservice.product.constant.SalesStatus;
import com.ecommerce.productservice.product.controller.internal.dto.response.ProductValidationResponse;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.command.ProductCreateContext;
import com.ecommerce.productservice.product.query.ProductDetailInfo;
import com.ecommerce.productservice.product.query.ProductInfo;
import com.ecommerce.productservice.product.command.ProductUpdateContext;
import com.ecommerce.productservice.product.command.VariantCommand;

import java.util.List;
import java.util.UUID;

public interface TenantProductService {
    Product createProduct(ProductCreateContext context);
    Product updateProduct(Long tenantId, Long productId, ProductUpdateContext context);
    void deleteProduct(Long tenantId, Long productId);
    void changeSalesStatus(Long tenantId, Long productId, SalesStatus newStatus);
    void setFeatured(Long tenantId, Long productId, boolean featured);
    Product getProductByIdAndTenantId(Long productId, Long tenantId);
    ProductInfo getProductInfoByIdAndTenantId(Long productId, Long tenantId);
    ProductDetailInfo getProductDetail(Long productId, Long tenantId);
    // q: ürün adı/SKU araması (null/boş = filtre yok). salesStatus: ON_SALE/OUT_OF_STOCK/COMING_SOON (null = hepsi).
    // sort: newest | sales | views | price_asc | price_desc (null/boş = newest).
    PageResponse<ProductInfo> getTenantProducts(Long tenantId, String q, String salesStatus, String sort, int page, int size);

    // ─── Varyant (child product) yönetimi ───────────────────────────────
    List<Product> getVariants(Long tenantId, Long parentProductId);
    Product createVariant(Long tenantId, Long parentProductId, VariantCommand command, UUID keycloakId);
    List<Product> createVariantsBatch(Long tenantId, Long parentProductId, List<VariantCommand> commands, UUID keycloakId);
    Product updateVariant(Long tenantId, Long variantId, VariantCommand command);
}
