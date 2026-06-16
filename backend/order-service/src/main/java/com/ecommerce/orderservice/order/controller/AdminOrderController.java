package com.ecommerce.orderservice.order.controller;

import com.ecommerce.orderservice.common.constants.ApiPaths;
import com.ecommerce.orderservice.order.constant.OrderStatus;
import com.ecommerce.orderservice.order.controller.dto.request.ResolveReturnRequest;
import com.ecommerce.orderservice.order.controller.dto.response.AdminOrderDetailResponse;
import com.ecommerce.orderservice.order.controller.dto.response.AdminOrderStatsResponse;
import com.ecommerce.orderservice.order.controller.dto.response.AdminOrderSummaryResponse;
import com.ecommerce.orderservice.order.controller.dto.response.ProductSalesMetricsResponse;
import com.ecommerce.orderservice.order.controller.dto.response.ReturnResponse;
import com.ecommerce.orderservice.order.mapper.OrderMapper;
import com.ecommerce.orderservice.order.service.AdminOrderService;
import com.ecommerce.orderservice.order.service.OrderReturnService;
import com.ecommerce.orderservice.order.service.ProductMetricsService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('platform-admin')")
@Tag(name = "Admin — Orders", description = "Platform admin order management — list, detail, stats (requires platform-admin role)")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;
    private final ProductMetricsService productMetricsService;
    private final OrderReturnService orderReturnService;
    private final OrderMapper orderMapper;

    @Operation(summary = "List all orders", description = "Paginated platform-wide orders. Optional status + tenantId filters.")
    @ApiResponse(responseCode = "200", description = "Order page")
    @GetMapping(ApiPaths.ADMIN_ORDERS)
    public ResponseEntity<Page<AdminOrderSummaryResponse>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(adminOrderService.listOrders(status, tenantId, pageable));
    }

    @Operation(summary = "Get order detail", description = "Full order with items for the admin order view.")
    @ApiResponse(responseCode = "200", description = "Order detail")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @GetMapping(ApiPaths.ADMIN_ORDER_DETAIL)
    public ResponseEntity<AdminOrderDetailResponse> getOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminOrderService.getOrderDetail(orderId));
    }

    @Operation(summary = "Order statistics", description = "Counts by status, total orders and total GMV for dashboards.")
    @ApiResponse(responseCode = "200", description = "Order stats")
    @GetMapping(ApiPaths.ADMIN_ORDER_STATS)
    public ResponseEntity<AdminOrderStatsResponse> getStats() {
        return ResponseEntity.ok(adminOrderService.getStats());
    }

    @Operation(summary = "Product sales metrics (admin)", description = "Platform geneli tek ürünün satış metriği — satılan adet/ciro/sipariş + varyant kırılımı. Tenant filtresi yok.")
    @ApiResponse(responseCode = "200", description = "Ürün satış metriği")
    @GetMapping(ApiPaths.ADMIN_PRODUCT_METRICS)
    public ResponseEntity<ProductSalesMetricsResponse> getProductMetrics(@PathVariable Long productId) {
        return ResponseEntity.ok(productMetricsService.getMetrics(productId, null));
    }

    // ─── İade override (admin) — merchant takılırsa admin karar verir ───
    @Operation(summary = "List all pending returns", description = "Platform geneli bekleyen (REQUESTED) iade talepleri.")
    @ApiResponse(responseCode = "200", description = "İade listesi")
    @GetMapping(ApiPaths.ADMIN_RETURNS)
    public ResponseEntity<List<ReturnResponse>> getReturns() {
        List<ReturnResponse> response = orderReturnService.getAllReturns().stream()
                .map(orderMapper::toReturnResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Approve return (admin override)", description = "Admin iadeyi onaylar → gerçek iyzico para iadesi + stok geri eklenir.")
    @ApiResponse(responseCode = "200", description = "İade onaylandı")
    @PostMapping(ApiPaths.ADMIN_RETURN_APPROVE)
    public ResponseEntity<Void> approveReturn(
            @PathVariable Long orderId,
            @RequestBody(required = false) ResolveReturnRequest request) {
        String note = (request != null) ? request.note() : null;
        orderReturnService.approveReturn(orderId, null, note); // tenantId null → admin
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reject return (admin override)", description = "Admin iade talebini reddeder → sipariş DELIVERED'a döner.")
    @ApiResponse(responseCode = "200", description = "İade reddedildi")
    @PostMapping(ApiPaths.ADMIN_RETURN_REJECT)
    public ResponseEntity<Void> rejectReturn(
            @PathVariable Long orderId,
            @RequestBody(required = false) ResolveReturnRequest request) {
        String note = (request != null) ? request.note() : null;
        orderReturnService.rejectReturn(orderId, null, note);
        return ResponseEntity.ok().build();
    }
}
