package com.ecommerce.productservice.product.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.productservice.common.constants.ApiPaths;
import com.ecommerce.productservice.common.service.ImageService;
import com.ecommerce.productservice.product.constant.SalesStatus;
import com.ecommerce.productservice.product.controller.dto.request.ProductUpdateRequest;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.command.ProductCreateContext;
import com.ecommerce.productservice.product.command.VariantCommand;
import com.ecommerce.productservice.product.controller.dto.request.ProductCreateRequest;
import com.ecommerce.productservice.product.controller.dto.request.VariantBatchRequest;
import com.ecommerce.productservice.product.controller.dto.request.VariantRequest;
import com.ecommerce.productservice.product.controller.dto.response.ProductDetailResponse;
import com.ecommerce.productservice.product.controller.dto.response.ProductResponse;
import com.ecommerce.productservice.product.controller.dto.response.VariantResponse;
import com.ecommerce.productservice.product.query.ProductDetailInfo;
import com.ecommerce.productservice.product.query.ProductInfo;
import com.ecommerce.productservice.product.command.ProductUpdateContext;
import com.ecommerce.productservice.product.mapper.ProductMapper;
import com.ecommerce.productservice.product.service.TenantProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "Tenant Products", description = "Merchant product catalog — create, update, delete products and upload images")
@RestController
@RequestMapping(ApiPaths.TenantProduct.TENANT_PRODUCTS)
@RequiredArgsConstructor
public class TenantProductController {

    private final TenantProductService tenantProductService;
    private final ProductMapper productMapper;
    private final ImageService imageService;

    @Operation(summary = "Create product", description = "Creates a new product in the tenant's catalog. Product is visible to customers once published. Triggers PRODUCT_CREATED event for search indexing.")
    @ApiResponse(responseCode = "200", description = "Product created")
    @ApiResponse(responseCode = "400", description = "Validation error — check SKU uniqueness, required fields")
    @ApiResponse(responseCode = "403", description = "Not authorized for this tenant")
    @Idempotent(cachePrefix = "idempotency:product-create:", ttlSeconds = 300)
    @PostMapping
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<ProductResponse> createProduct(
            @PathVariable Long tenantId,
            @Valid @RequestBody ProductCreateRequest request,
            @CurrentUser AuthUser user) {

        ProductCreateContext context = productMapper.toContext(request, tenantId, user.keycloakId());
        Product saved = tenantProductService.createProduct(context);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productMapper.toResponse(saved));
    }

    @Operation(summary = "List tenant products", description = "Paginated list of all products in this tenant's catalog, including draft and inactive products.")
    @ApiResponse(responseCode = "200", description = "Paginated product list")
    // GET /api/v1/products/tenants/{tenantId}?page=0&size=20
    @GetMapping
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<PageResponse<ProductResponse>> getTenantProducts(
            @PathVariable Long tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String salesStatus,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<ProductInfo> result =
                tenantProductService.getTenantProducts(tenantId, q, salesStatus, sort, page, size);

        PageResponse<ProductResponse> response = new PageResponse<>(
                result.content().stream()
                        .map(productMapper::toResponseFromInfo)
                        .toList(),
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.isLast()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get product", description = "Full product detail for the merchant view — includes all fields, stock status, SEO data.")
    @ApiResponse(responseCode = "200", description = "Product detail")
    @ApiResponse(responseCode = "404", description = "Product not found in this tenant")
    @GetMapping("/{productId}")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<ProductResponse> getTenantProduct(
            @PathVariable Long tenantId,
            @PathVariable Long productId) {

        ProductInfo info = tenantProductService
                .getProductInfoByIdAndTenantId(productId, tenantId);
        return ResponseEntity.ok(productMapper.toResponseFromInfo(info));
    }

    @Operation(summary = "Get product detail (edit view)", description = "Extended product detail for the edit form — includes all editable fields: SEO, tags, dimensions, variants.")
    @ApiResponse(responseCode = "200", description = "Extended product detail")
    @GetMapping("/{productId}/detail")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<ProductDetailResponse> getTenantProductDetail(
            @PathVariable Long tenantId,
            @PathVariable Long productId) {

        ProductDetailInfo info = tenantProductService.getProductDetail(productId, tenantId);
        return ResponseEntity.ok(productMapper.toDetailResponse(info));
    }

    @Operation(summary = "Update product", description = "Updates all product fields. Triggers PRODUCT_UPDATED event for search re-indexing.")
    @ApiResponse(responseCode = "200", description = "Product updated")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @Idempotent(cachePrefix = "idempotency:product-update:")
    @PutMapping("/{productId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long tenantId,
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @CurrentUser AuthUser user) {

        ProductUpdateContext context =
                productMapper.toUpdateContext(request, tenantId, user.keycloakId());
        Product updated = tenantProductService.updateProduct(tenantId, productId, context);
        return ResponseEntity.ok(productMapper.toResponse(updated));
    }

    @Operation(summary = "Change sales status", description = "Toggles product between ON_SALE, PAUSED, OUT_OF_STOCK. Affects customer-facing visibility.")
    @ApiResponse(responseCode = "200", description = "Status changed")
    // PATCH — sadece satış durumunu değiştir, tüm ürünü yollama
    @PatchMapping("/{productId}/sales-status")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> changeSalesStatus(
            @PathVariable Long tenantId,
            @PathVariable Long productId,
            @RequestParam SalesStatus status) {

        tenantProductService.changeSalesStatus(tenantId, productId, status);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Toggle featured", description = "Ürünü öne çıkar/kaldır. Öne çıkan ürünler storefront vitrininde gösterilir. PRODUCT_UPDATED event'i ile ES'e yansır.")
    @ApiResponse(responseCode = "200", description = "Öne çıkan durumu güncellendi")
    @PatchMapping("/{productId}/featured")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> setFeatured(
            @PathVariable Long tenantId,
            @PathVariable Long productId,
            @RequestParam boolean featured) {

        tenantProductService.setFeatured(tenantId, productId, featured);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete product", description = "Soft-deletes the product (status → DELETED). Triggers PRODUCT_DELETED event to remove from search index.")
    @ApiResponse(responseCode = "200", description = "Product deleted")
    @DeleteMapping("/{productId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long tenantId,
            @PathVariable Long productId) {

        tenantProductService.deleteProduct(tenantId, productId);
        return ResponseEntity.noContent().build();
    }

    // ─── Varyant (child product) yönetimi ───────────────────────────────

    @Operation(summary = "List product variants", description = "Bir ürünün varyantlarını (child) listeler. Varyantlar ana ürün listesinde görünmez.")
    @ApiResponse(responseCode = "200", description = "Varyant listesi")
    @GetMapping("/{productId}/variants")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<List<VariantResponse>> getVariants(
            @PathVariable Long tenantId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(productMapper.toVariantResponses(
                tenantProductService.getVariants(tenantId, productId)));
    }

    @Operation(summary = "Create variant", description = "Ana ürünün altına varyant (child) ekler. Kombinasyon (attributes) + SKU + fiyat zorunlu. Stok ayrıca stock-service'ten girilir.")
    @ApiResponse(responseCode = "201", description = "Varyant oluşturuldu")
    @ApiResponse(responseCode = "409", description = "Aynı kombinasyonda varyant zaten var")
    @Idempotent(cachePrefix = "idempotency:variant-create:", ttlSeconds = 300)
    @PostMapping("/{productId}/variants")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<VariantResponse> createVariant(
            @PathVariable Long tenantId,
            @PathVariable Long productId,
            @Valid @RequestBody VariantRequest request,
            @CurrentUser AuthUser user) {

        Product saved = tenantProductService.createVariant(
                tenantId, productId, toVariantCommand(request), user.keycloakId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productMapper.toVariantResponse(saved));
    }

    @Operation(summary = "Create variants in batch", description = "Matris üretici: tek istekte N varyant. Eksen kombinasyonlarından üretilmiş varyantları toplu ekler. Atomik — biri (ör. kombinasyon çakışması) patlarsa hiçbiri eklenmez.")
    @ApiResponse(responseCode = "201", description = "Varyantlar oluşturuldu")
    @ApiResponse(responseCode = "409", description = "Kombinasyonlardan biri zaten var")
    @Idempotent(cachePrefix = "idempotency:variant-batch:", ttlSeconds = 300)
    @PostMapping("/{productId}/variants/batch")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<List<VariantResponse>> createVariantsBatch(
            @PathVariable Long tenantId,
            @PathVariable Long productId,
            @Valid @RequestBody VariantBatchRequest request,
            @CurrentUser AuthUser user) {

        List<VariantCommand> commands = request.variants().stream()
                .map(this::toVariantCommand)
                .toList();
        List<Product> saved = tenantProductService.createVariantsBatch(
                tenantId, productId, commands, user.keycloakId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productMapper.toVariantResponses(saved));
    }

    @Operation(summary = "Update variant", description = "Varyantın fiyat/kombinasyon/görselini günceller.")
    @ApiResponse(responseCode = "200", description = "Varyant güncellendi")
    @PutMapping("/variants/{variantId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<VariantResponse> updateVariant(
            @PathVariable Long tenantId,
            @PathVariable Long variantId,
            @Valid @RequestBody VariantRequest request) {

        Product updated = tenantProductService.updateVariant(tenantId, variantId, toVariantCommand(request));
        return ResponseEntity.ok(productMapper.toVariantResponse(updated));
    }

    @Operation(summary = "Delete variant", description = "Varyantı soft-delete eder (status → DELETED).")
    @ApiResponse(responseCode = "204", description = "Varyant silindi")
    @DeleteMapping("/variants/{variantId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> deleteVariant(
            @PathVariable Long tenantId,
            @PathVariable Long variantId) {
        tenantProductService.deleteProduct(tenantId, variantId);
        return ResponseEntity.noContent().build();
    }

    private VariantCommand toVariantCommand(VariantRequest r) {
        return new VariantCommand(
                r.name(), r.sku(), r.price(), r.discountedPrice(), r.mainImageUrl(), r.attributes());
    }

    @Operation(summary = "Upload product image", description = "Uploads an image to MinIO products/ bucket. Returns the public URL to use in product create/update requests. Max 5MB, JPEG/PNG/WebP.")
    @ApiResponse(responseCode = "200", description = "Image uploaded — response contains {\"url\": \"http://...\"}")
    @PostMapping("/images/upload")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Map<String, String>> uploadProductImage(
            @PathVariable Long tenantId,
            @RequestParam("file") MultipartFile file) {

        String url = imageService.uploadImage(file, "products");
        return ResponseEntity.ok(Map.of("url", url));
    }
}