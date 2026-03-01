package com.ecommerce.paymentservice.payment.mapper;

import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.controller.dto.request.PaymentRequest;
import com.ecommerce.paymentservice.payment.controller.dto.response.PaymentHistoryResponse;
import com.ecommerce.paymentservice.payment.controller.dto.response.PaymentResponse;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.domain.PaymentCardInfo;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
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

    @Mapping(source = "type", target = "type")
    PaymentContext toContext(PaymentRequest request);

    @Named("mapSuccess")
    default boolean mapSuccess(PaymentStatus status) {
        return status == PaymentStatus.SUCCESS;
    }

    default PaymentHistoryResponse toPaymentHistoryResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        LocalDateTime txDate = payment.getPaymentStatus() == PaymentStatus.SUCCESS
                ? payment.getPaidAt()
                : payment.getFailedAt();

        String desc = payment.getPaymentType() == PaymentType.SUBSCRIPTION
                ? "Abonelik Ödemesi"
                : "Sipariş #" + payment.getOrderId();

        return new PaymentHistoryResponse(
                payment.getId(),
                payment.getPaymentType(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentStatus(),
                txDate,
                desc,
                payment.getFailureReason(),
                payment.getPaymentMethod()
        );
    }

}