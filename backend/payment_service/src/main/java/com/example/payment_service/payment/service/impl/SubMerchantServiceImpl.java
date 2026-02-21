package com.example.payment_service.payment.service.impl;

import com.example.payment_service.payment.controller.dto.request.SubMerchantCreateRequest;
import com.example.payment_service.payment.controller.dto.request.SubMerchantUpdateRequest;
import com.example.payment_service.payment.service.SubMerchantService;
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
        SubMerchant subMerchant = SubMerchant.create(request, iyzicoOptions);

        if ("success".equalsIgnoreCase(subMerchant.getStatus())){
            return subMerchant.getSubMerchantKey();
        }else{
            log.error("Iyzico SubMerchant Oluşturma Hatası: {}", subMerchant.getErrorMessage());
            throw new RuntimeException("Alt üye iş yeri açılamadı: " + subMerchant.getErrorMessage());
        }

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

        SubMerchant subMerchant = SubMerchant.update(request, iyzicoOptions);

        if ("success".equalsIgnoreCase(subMerchant.getStatus())) {
            log.info("SubMerchant başarıyla güncellendi. Key: {}", dto.existingSubMerchantKey());
            return dto.existingSubMerchantKey();
        } else {
            log.error("Iyzico SubMerchant Güncelleme Hatası: {}", subMerchant.getErrorMessage());
            throw new RuntimeException("Alt üye işyeri güncellenemedi: " + subMerchant.getErrorMessage());
        }

    }

    private String formatPhone(String phone) {
        if (phone == null) return "+905555555555";
        if (phone.startsWith("+")) return phone;
        if (phone.startsWith("0")) return "+9" + phone;
        return "+90" + phone;
    }
}
