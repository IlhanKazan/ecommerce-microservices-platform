package com.example.payment_service.payment.controller;

import com.example.payment_service.common.constants.ApiPaths;
import com.example.payment_service.payment.controller.dto.request.SubMerchantCreateRequest;
import com.example.payment_service.payment.controller.dto.request.SubMerchantUpdateRequest;
import com.example.payment_service.payment.controller.dto.response.SubMerchantResponse;
import com.example.payment_service.payment.service.SubMerchantService;
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
