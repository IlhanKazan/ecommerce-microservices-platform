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
public class OrderController {

    private final OrderSagaService orderSagaService;
    private final OrderQueryService orderQueryService;
    private final OrderStatusService orderStatusService;
    private final OrderCancelService orderCancelService;
    private final OrderMapper orderMapper;

    // ─── Checkout ─────────────────────────────────────────────────────────
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

    @GetMapping(ApiPaths.MY_ORDER_DETAIL)
    public ResponseEntity<OrderDetailResponse> getMyOrderDetail(
            @PathVariable Long orderId,
            @CurrentUser AuthUser user) {

        OrderInfo info = orderQueryService.getMyOrderDetail(orderId, user.keycloakId());
        return ResponseEntity.ok(orderMapper.toDetailResponse(info));
    }

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
