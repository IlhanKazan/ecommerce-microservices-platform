package com.example.payment_service.payment.mapper;

import com.example.payment_service.payment.controller.dto.request.PaymentRequest;
import com.example.payment_service.payment.controller.dto.response.PaymentResponse;
import com.example.payment_service.payment.entity.Payment;
import com.example.payment_service.payment.constant.PaymentStatus;
import com.example.payment_service.payment.domain.PaymentCardInfo;
import com.example.payment_service.payment.domain.PaymentContext;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "customerId", target = "customerId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "netAmount", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "paymentType", ignore = true)
    Payment toEntity(PaymentRequest request);


    // PaymentStatus enum olduğu için boolean success dönüşümü için özel metod yazdık
    @Mapping(source = "paymentStatus", target = "success", qualifiedByName = "mapSuccess")
    @Mapping(source = "failureReason", target = "message")
    @Mapping(source = "id", target = "id")
    PaymentResponse toResponse(Payment payment);

    PaymentCardInfo toCardInfo(PaymentRequest request);

    PaymentContext toContext(PaymentRequest request);

    @Named("mapSuccess")
    default boolean mapSuccess(PaymentStatus status) {
        return status == PaymentStatus.SUCCESS;
    }
}