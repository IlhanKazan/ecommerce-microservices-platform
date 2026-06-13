package com.ecommerce.paymentservice.payment.service.impl;

import com.ecommerce.common.exception.ExternalServiceException;
import com.ecommerce.paymentservice.payment.controller.dto.request.SubMerchantCreateRequest;
import com.ecommerce.paymentservice.payment.controller.dto.request.SubMerchantUpdateRequest;
import com.ecommerce.paymentservice.payment.service.SubMerchantService;
import com.iyzipay.Options;
import com.iyzipay.model.Currency;
import com.iyzipay.model.Locale;
import com.iyzipay.model.SubMerchant;
import com.iyzipay.model.SubMerchantType;
import com.iyzipay.request.CreateSubMerchantRequest;
import com.iyzipay.request.UpdateSubMerchantRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubMerchantServiceImpl implements SubMerchantService {

    private final Options iyzicoOptions;

    @Override
    public String createSubMerchant(SubMerchantCreateRequest dto) {
        // iyzico request
        CreateSubMerchantRequest request = new CreateSubMerchantRequest();
        request.setSubMerchantExternalId(dto.tenantId().toString());

        request.setLocale(Locale.TR.getValue());
        request.setAddress(dto.address());
        request.setEmail(dto.email());
        request.setGsmNumber(formatPhone(dto.phone()));
        request.setName(dto.legalCompanyTitle());
        request.setContactName(dto.contactName());
        request.setContactSurname(dto.contactSurname());
        request.setIban(dto.iban());
        request.setCurrency(Currency.TRY.name());
        request.setLegalCompanyTitle(dto.legalCompanyTitle());
        request.setTaxOffice(dto.taxOffice());

        if("CORPORATE".equals(dto.businessType())){
            request.setSubMerchantType(SubMerchantType.LIMITED_OR_JOINT_STOCK_COMPANY.name());
            request.setTaxNumber(dto.taxId());
        }else{
            request.setSubMerchantType(SubMerchantType.PRIVATE_COMPANY.name());
            request.setIdentityNumber(dto.taxId());
        }

        log.info("SUB MERCHANT CREATE REQUEST >> {}", request);
        // iyzicoya gönder
        SubMerchant subMerchant = callIyzicoCreate(request);

        if ("success".equalsIgnoreCase(subMerchant.getStatus())){
            String key = subMerchant.getSubMerchantKey();
            if (key == null || key.isBlank()) {
                // iyzico "success" dedi ama key boş — güvenmiyoruz, hata sayıyoruz
                log.error("Iyzico submerchant 'success' döndü ama subMerchantKey boş. tenantId: {}", dto.tenantId());
                throw new ExternalServiceException("Alt üye iş yeri anahtarı alınamadı.", "SUBMERCHANT_KEY_MISSING");
            }
            return key;
        }
        log.error("Iyzico SubMerchant Oluşturma Hatası — code: {}, msg: {}",
                subMerchant.getErrorCode(), subMerchant.getErrorMessage());
        throw new ExternalServiceException(
                "Alt üye iş yeri açılamadı: " + iyzicoError(subMerchant),
                "SUBMERCHANT_CREATE_FAILED");
    }

    private SubMerchant callIyzicoCreate(CreateSubMerchantRequest request) {
        try {
            return SubMerchant.create(request, iyzicoOptions);
        } catch (Exception e) {
            log.error("Iyzico submerchant create çağrısı hata fırlattı: {}", e.getMessage(), e);
            throw new ExternalServiceException("iyzico'ya ulaşılamadı: " + e.getMessage(), "IYZICO_UNAVAILABLE");
        }
    }

    private String iyzicoError(SubMerchant subMerchant) {
        String msg = subMerchant.getErrorMessage();
        return msg != null ? msg : "bilinmeyen iyzico hatası";
    }

    @Override
    public String updateSubMerchant(SubMerchantUpdateRequest dto) {

        UpdateSubMerchantRequest request = new UpdateSubMerchantRequest();
        request.setSubMerchantKey(dto.existingSubMerchantKey());

        request.setLocale(Locale.TR.getValue());
        request.setAddress(dto.address());
        request.setEmail(dto.email());
        request.setGsmNumber(formatPhone(dto.phone()));
        request.setName(dto.legalCompanyTitle());
        request.setContactName(dto.contactName());
        request.setContactSurname(dto.contactSurname());
        request.setIban(dto.iban());
        request.setCurrency(Currency.TRY.name());
        request.setLegalCompanyTitle(dto.legalCompanyTitle());
        request.setTaxOffice(dto.taxOffice());

        if ("CORPORATE".equals(dto.businessType())) {
            request.setTaxNumber(dto.taxId());
        } else {
            request.setIdentityNumber(dto.taxId());
        }

        SubMerchant subMerchant;
        try {
            subMerchant = SubMerchant.update(request, iyzicoOptions);
        } catch (Exception e) {
            log.error("Iyzico submerchant update çağrısı hata fırlattı: {}", e.getMessage(), e);
            throw new ExternalServiceException("iyzico'ya ulaşılamadı: " + e.getMessage(), "IYZICO_UNAVAILABLE");
        }

        if ("success".equalsIgnoreCase(subMerchant.getStatus())) {
            log.info("SubMerchant başarıyla güncellendi. Key: {}", dto.existingSubMerchantKey());
            return dto.existingSubMerchantKey();
        }
        log.error("Iyzico SubMerchant Güncelleme Hatası — code: {}, msg: {}",
                subMerchant.getErrorCode(), subMerchant.getErrorMessage());
        throw new ExternalServiceException(
                "Alt üye işyeri güncellenemedi: " + iyzicoError(subMerchant),
                "SUBMERCHANT_UPDATE_FAILED");
    }

    private String formatPhone(String phone) {
        if (phone == null) return "+905555555555";
        if (phone.startsWith("+")) return phone;
        if (phone.startsWith("0")) return "+9" + phone;
        return "+90" + phone;
    }
}
