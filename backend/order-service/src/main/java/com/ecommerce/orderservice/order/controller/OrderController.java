package com.ecommerce.orderservice.order.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.orderservice.common.constants.ApiPaths;
import com.ecommerce.orderservice.order.command.CheckoutCommand;
import com.ecommerce.orderservice.order.command.UpdateOrderStatusCommand;
import com.ecommerce.orderservice.order.controller.dto.request.CancelOrderRequest;
import com.ecommerce.orderservice.order.controller.dto.request.CheckoutRequest;
import com.ecommerce.orderservice.order.controller.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.orderservice.order.controller.dto.response.OrderDetailResponse;
import com.ecommerce.orderservice.order.controller.dto.response.OrderResponse;
import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.mapper.OrderMapper;
import com.ecommerce.orderservice.order.query.OrderInfo;
import com.ecommerce.orderservice.order.service.OrderCancelService;
import com.ecommerce.orderservice.order.service.OrderQueryService;
import com.ecommerce.orderservice.order.service.OrderSagaService;
import com.ecommerce.orderservice.order.service.OrderStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order lifecycle — checkout (Pay-First SAGA), customer order history, merchant order management, status updates, cancellation")
public class OrderController {

    private final OrderSagaService orderSagaService;
    private final OrderQueryService orderQueryService;
    private final OrderStatusService orderStatusService;
    private final OrderCancelService orderCancelService;
    private final OrderMapper orderMapper;

    // ─── Checkout ─────────────────────────────────────────────────────────
    @Operation(
        summary = "Create order (checkout)",
        description = "Initiates the Pay-First SAGA: (1) reserve stock, (2) process iyzico payment, (3) persist confirmed order. " +
            "Atomic — if payment fails, reserved stock is automatically released. " +
            "Idempotent — include X-Idempotency-Key header to prevent double-charge on network retry. " +
            "Price is computed from product snapshots server-side — basket price is NOT trusted."
    )
    @ApiResponse(responseCode = "201", description = "Order created and payment confirmed")
    @ApiResponse(responseCode = "402", description = "Payment rejected by iyzico")
    @ApiResponse(responseCode = "409", description = "Duplicate request — idempotency key already processed (order exists)")
    @ApiResponse(responseCode = "422", description = "Insufficient stock for one or more items")
    @ApiResponse(responseCode = "400", description = "Sub-merchant key not configured for this tenant")
    @Idempotent(cachePrefix = "idempotency:checkout:", ttlSeconds = 300)
    @PostMapping(ApiPaths.TENANT_ORDERS)
    public ResponseEntity<OrderResponse> checkout(
            @PathVariable Long tenantId,
            @Valid @RequestBody CheckoutRequest request,
            @CurrentUser AuthUser user) {

        CheckoutCommand command = new CheckoutCommand(
                user.keycloakId(),
                tenantId,
                request.shippingAddressJson(),
                request.cardInfo(),
                request.buyer(),
                request.billingAddress(),
                user.email()
        );

        Order order = orderSagaService.checkout(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toResponse(order));
    }

    // ─── Kullanıcı - kendi siparişleri ──────────────────────────────────
    @Operation(summary = "List my orders", description = "Returns paginated order history for the authenticated customer, ordered by most recent.")
    @ApiResponse(responseCode = "200", description = "Paginated order list")
    @GetMapping(ApiPaths.MY_ORDERS)
    public ResponseEntity<PageResponse<OrderDetailResponse>> getMyOrders(
            @CurrentUser AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<OrderInfo> result = orderQueryService.getMyOrders(user.keycloakId(), page, size);
        PageResponse<OrderDetailResponse> response = new PageResponse<>(
                result.content().stream().map(orderMapper::toDetailResponse).toList(),
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.isLast()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get order detail", description = "Full order detail including items, shipping address, payment status and timeline.")
    @ApiResponse(responseCode = "200", description = "Order detail")
    @ApiResponse(responseCode = "403", description = "Order does not belong to this user")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @GetMapping(ApiPaths.MY_ORDER_DETAIL)
    public ResponseEntity<OrderDetailResponse> getMyOrderDetail(
            @PathVariable Long orderId,
            @CurrentUser AuthUser user) {

        OrderInfo info = orderQueryService.getMyOrderDetail(orderId, user.keycloakId());
        return ResponseEntity.ok(orderMapper.toDetailResponse(info));
    }

    @Operation(summary = "Cancel order", description = "Customer cancels their own order. Only CONFIRMED or PENDING orders can be cancelled. Triggers automatic refund via payment-service and stock release.")
    @ApiResponse(responseCode = "200", description = "Order cancelled and refund initiated")
    @ApiResponse(responseCode = "422", description = "Order cannot be cancelled — already shipped or delivered")
    @ApiResponse(responseCode = "403", description = "Order does not belong to this user")
    @PostMapping(ApiPaths.MY_ORDER_CANCEL)
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) CancelOrderRequest request,
            @CurrentUser AuthUser user) {
        String reason = (request != null && request.reason() != null) ? request.reason() : "Kullanıcı iptali";
        Order cancelled = orderCancelService.cancelOrder(orderId, user.keycloakId(), reason, user.email());
        return ResponseEntity.ok(orderMapper.toResponse(cancelled));
    }

    // ─── Merchant - mağaza siparişleri ──────────────────────────────────
    @Operation(summary = "List tenant orders", description = "Merchant views all orders placed in their store. Paginated, ordered by most recent.")
    @ApiResponse(responseCode = "200", description = "Paginated order list")
    @ApiResponse(responseCode = "403", description = "Not authorized for this tenant")
    @GetMapping(ApiPaths.TENANT_ORDERS)
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<PageResponse<OrderDetailResponse>> getTenantOrders(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<OrderInfo> result = orderQueryService.getTenantOrders(tenantId, page, size);
        PageResponse<OrderDetailResponse> response = new PageResponse<>(
                result.content().stream().map(orderMapper::toDetailResponse).toList(),
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.isLast()
        );
        return ResponseEntity.ok(response);
    }

    // ─── Merchant - sipariş durumu güncelle ─────────────────────────────
    @Operation(summary = "Update order status", description = "Merchant updates order to SHIPPED (with tracking number) or DELIVERED. " +
        "SHIPPED triggers ORDER_SHIPPED_EVENT → customer email. " +
        "DELIVERED triggers ORDER_DELIVERED_EVENT → customer email.")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "400", description = "Invalid status transition — valid values: SHIPPED, DELIVERED")
    @ApiResponse(responseCode = "403", description = "Not authorized for this tenant")
    @ApiResponse(responseCode = "404", description = "Order not found in this tenant")
    @PutMapping(ApiPaths.TENANT_ORDER_STATUS)
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long tenantId,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(
                orderId, tenantId, request.status(), request.trackingNumber());
        Order updated = orderStatusService.updateStatus(command);
        return ResponseEntity.ok(orderMapper.toResponse(updated));
    }
}
