package com.ecommerce.orderservice.order.mapper;

import com.ecommerce.orderservice.order.controller.dto.response.OrderDetailResponse;
import com.ecommerce.orderservice.order.controller.dto.response.OrderResponse;
import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.query.OrderInfo;
import com.ecommerce.orderservice.order.query.OrderItemInfo;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface OrderMapper {

    @Mapping(source = "id", target = "orderId")
    @Mapping(expression = "java(order.getStatus().name())", target = "status")
    @Mapping(source = "createdAt", target = "createdAt")
    OrderResponse toResponse(Order order);

    default OrderDetailResponse toDetailResponse(OrderInfo info) {
        if (info == null) return null;
        List<OrderDetailResponse.OrderItemDetailDto> items = info.items() == null ? List.of() :
                info.items().stream()
                        .map(this::toItemDetailDto)
                        .toList();
        return new OrderDetailResponse(
                info.id(),
                info.status(),
                info.totalAmount(),
                info.currency(),
                info.shippingAddressJson(),
                info.createdAt(),
                items
        );
    }

    default OrderDetailResponse.OrderItemDetailDto toItemDetailDto(OrderItemInfo item) {
        return new OrderDetailResponse.OrderItemDetailDto(
                item.productId(), item.sku(), item.productName(),
                item.productImageUrl(), item.unitPrice(), item.quantity()
        );
    }
}
