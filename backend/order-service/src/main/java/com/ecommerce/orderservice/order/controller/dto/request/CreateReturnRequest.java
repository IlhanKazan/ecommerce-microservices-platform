package com.ecommerce.orderservice.order.controller.dto.request;

/** Müşteri iade talebi — yapılandırılmış sebep kodu + opsiyonel serbest not. */
public record CreateReturnRequest(String reasonCode, String note) {}
