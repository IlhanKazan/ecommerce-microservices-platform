package com.ecommerce.usertenantservice.common.client;

import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentResponse;
import com.ecommerce.usertenantservice.tenant.domain.PaymentProcessRequest;
import org.springframework.stereotype.Component;

@Component
public class PaymentServiceFallback implements PaymentServiceClient{

    @Override
    public PaymentResponse processPayment(PaymentProcessRequest request){
        return new PaymentResponse(0L, false, "SERVICE_UNAVAILABLE");
    }

}
