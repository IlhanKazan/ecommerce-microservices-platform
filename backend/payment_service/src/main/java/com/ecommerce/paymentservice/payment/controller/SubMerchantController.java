package com.ecommerce.paymentservice.payment.controller;

import com.ecommerce.paymentservice.common.constants.ApiPaths;
import com.ecommerce.paymentservice.payment.controller.dto.request.SubMerchantCreateRequest;
import com.ecommerce.paymentservice.payment.controller.dto.request.SubMerchantUpdateRequest;
import com.ecommerce.paymentservice.payment.controller.dto.response.SubMerchantResponse;
import com.ecommerce.paymentservice.payment.service.SubMerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.SubMerchant.SUBMERCHANT)
@RequiredArgsConstructor
public class SubMerchantController {

    private final SubMerchantService subMerchantService;

    @PostMapping("/create")
    public ResponseEntity<SubMerchantResponse> createSubMerchant(@RequestBody SubMerchantCreateRequest request){
        return ResponseEntity.ok(new SubMerchantResponse(subMerchantService.createSubMerchant(request)));
    }

    @PutMapping("/update")
    public ResponseEntity<SubMerchantResponse> updateSubMerchant(@RequestBody SubMerchantUpdateRequest request){
        return ResponseEntity.ok(new SubMerchantResponse(subMerchantService.updateSubMerchant(request)));
    }

}
