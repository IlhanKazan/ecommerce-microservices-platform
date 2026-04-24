package com.ecommerce.usertenantservice.integration.payment;

import com.ecommerce.usertenantservice.exception.PaymentServiceUnreachableException;
import com.ecommerce.usertenantservice.exception.UnexpectedErrorException;
import com.ecommerce.usertenantservice.exception.VerificationException;
import com.ecommerce.usertenantservice.integration.payment.dto.PaymentResult;
import com.ecommerce.usertenantservice.tenant.controller.dto.request.SubMerchantCreateRequest;
import com.ecommerce.usertenantservice.tenant.controller.dto.request.SubMerchantUpdateRequest;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentHistoryResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.SubMerchantResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantSubscriptionResponse;
import com.ecommerce.usertenantservice.tenant.domain.PaymentProcessRequest;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClientAdapter {

    private final PaymentServiceClient paymentServiceClient;
    private final String UNEXPECTED_ERROR = "Beklenmedik bir hata oluştu";

    public PaymentResult processPayment(PaymentProcessRequest request) {
        try {
            log.info("Sending payment request for TenantId: {}", request.tenantId());

            PaymentResponse response = paymentServiceClient.processPayment(request);

            if (response != null) {

                if (response.success()) {
                    return PaymentResult.success(response);
                } else {
                    log.warn("Payment Service returned business failure: {}", response.message());
                    return PaymentResult.failure(response.message());
                }
            } else {
                return PaymentResult.failure("HTTP Error: PaymentResponse is null");
            }

        } catch (FeignException e) {
            log.error("Feign communication error: Status: {}", e.status());
            // TODO [09.02.2026 23:54]: Burada detaylı hata analizi yapılabilir, bakiye yetersiz vs
            return PaymentResult.failure("Ödeme servisine ulaşılamadı veya hata aldı: " + e.status());
        } catch (Exception e) {
            log.error("Unexpected error during payment process", e);
            return PaymentResult.failure(UNEXPECTED_ERROR);
        }
    }

    public Optional<TenantSubscriptionResponse> getSubscriptionDetails(Long tenantId){
        try {
            log.info("Abonelik bilgisi çekiliyor. TenantId: {}", tenantId);

            TenantSubscriptionResponse response = paymentServiceClient.getSubscriptionDetails(tenantId);

            return Optional.ofNullable(response);

        } catch (FeignException.NotFound e) {
            log.info("TenantId: {} için abonelik kaydı bulunamadı.", tenantId);
            return Optional.empty();

        } catch (FeignException e) {
            log.error("Payment Service'e ulaşırken hata: Status: {}", e.status());
            throw new PaymentServiceUnreachableException("Ödeme servisi şu an yanıt vermiyor.");

        } catch (Exception e) {
            log.error("Abonelik çekilirken beklenmedik hata", e);
            throw new UnexpectedErrorException(UNEXPECTED_ERROR);
        }
    }

    public Page<PaymentHistoryResponse> getUserPaymentHistory(Long userId, Pageable pageable){
        try {
            log.info("Kullanıcı ödeme geçmişi çekiliyor: {}", userId);
            return paymentServiceClient.getUserPaymentHistory(userId, pageable);
        }catch (FeignException.NotFound e){
            log.info("Kullanıcının ödeme geçmişi bulunamadı");
            return null;
        }
    }

    public Page<PaymentHistoryResponse> getTenantPaymentHistory(Long tenantId, Pageable pageable) {
        try {
            log.info("Mağaza ödeme geçmişi çekiliyor: {}", tenantId);
            return paymentServiceClient.getTenantPaymentHistory(tenantId, pageable);
        } catch (FeignException.NotFound e) {
            log.info("Mağaza ödeme geçmişi bulunamadı");
            return null;
        }
    }

    public String createSubMerchant(SubMerchantCreateRequest request){
        try{
            SubMerchantResponse response = paymentServiceClient.createSubMerchant(request);
            return response.subMerchantKey();

        } catch (FeignException e) {
            log.error("Iyzico SubMerchant hatası. Status: {}, Body: {}", e.status(), e.contentUTF8());
            throw new VerificationException("Iyzico işlemi başarısız: " + e.contentUTF8());
        } catch (Exception e) {
            log.error("SubMerchant oluşturulurken sistemsel hata", e);
            throw new VerificationException("Beklenmedik bir hata oluştu.");
        }
    }

    public void updateSubMerchant(SubMerchantUpdateRequest request){
        try{
            paymentServiceClient.updateSubMerchant(request);

        } catch (FeignException e) {
            log.error("Iyzico SubMerchant hatası. Status: {}, Body: {}", e.status(), e.contentUTF8());
            throw new VerificationException("Iyzico işlemi başarısız: " + e.contentUTF8());
        } catch (Exception e) {
            log.error("SubMerchant güncellenirken sistemsel hata", e);
            throw new VerificationException("Beklenmedik bir hata oluştu.");
        }
    }

}