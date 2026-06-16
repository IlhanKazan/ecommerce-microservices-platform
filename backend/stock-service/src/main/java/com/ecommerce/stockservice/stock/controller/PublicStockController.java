package com.ecommerce.stockservice.stock.controller;

import com.ecommerce.stockservice.common.constants.ApiPaths;
import com.ecommerce.stockservice.stock.controller.dto.request.StockAvailabilityRequest;
import com.ecommerce.stockservice.stock.controller.dto.response.StockAvailabilityResponse;
import com.ecommerce.stockservice.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.Public.PUBLIC_STOCKS)
@RequiredArgsConstructor
@Tag(name = "Public Stock", description = "Anonymous stock availability — product/variant detail page out-of-stock + low-stock display")
public class PublicStockController {

    private final StockService stockService;

    @Operation(
            summary = "Variant/product availability",
            description = "Ürün/varyant id listesi için stok durumu. Detay sayfasında stoksuz varyantı çarpılı göstermek ve düşük stokta 'Son X adet' uyarısı için. availableQuantity yalnızca düşük stokta dolar.",
            security = {}
    )
    @ApiResponse(responseCode = "200", description = "Her id için inStock + (düşükse) availableQuantity")
    @PostMapping(ApiPaths.Public.AVAILABILITY)
    public ResponseEntity<List<StockAvailabilityResponse>> getAvailability(
            @Valid @RequestBody StockAvailabilityRequest request) {

        List<StockAvailabilityResponse> response = stockService.getAvailability(request.productIds()).stream()
                .map(info -> new StockAvailabilityResponse(
                        info.productId(),
                        info.inStock(),
                        info.availableQuantity()))
                .toList();

        return ResponseEntity.ok(response);
    }
}
