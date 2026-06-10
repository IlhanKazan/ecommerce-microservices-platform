package com.ecommerce.paymentservice.payment.controller;

import com.ecommerce.paymentservice.common.constants.ApiPaths;
import com.ecommerce.paymentservice.payment.controller.dto.request.SubMerchantCreateRequest;
import com.ecommerce.paymentservice.payment.controller.dto.request.SubMerchantUpdateRequest;
import com.ecommerce.paymentservice.payment.controller.dto.response.SubMerchantResponse;
import com.ecommerce.paymentservice.payment.service.SubMerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.SubMerchant.SUBMERCHANT)
@RequiredArgsConstructor
@Tag(name = "Sub-Merchants", description = "iyzico sub-merchant registration — required for marketplace commission model")
public class SubMerchantController {

    private final SubMerchantService subMerchantService;

    @Operation(summary = "Register sub-merchant", description = "Registers a new iyzico sub-merchant account for a tenant. Required before the tenant can receive marketplace payments.")
    @ApiResponse(responseCode = "200", description = "Sub-merchant registered")
    @PostMapping("/create")
    public ResponseEntity<SubMerchantResponse> createSubMerchant(@RequestBody SubMerchantCreateRequest request){
        return ResponseEntity.ok(new SubMerchantResponse(subMerchantService.createSubMerchant(request)));
    }

    @Operation(summary = "Update sub-merchant", description = "Updates sub-merchant details in iyzico (legal name, IBAN, address).")
    @ApiResponse(responseCode = "200", description = "Sub-merchant updated")
    @PutMapping("/update")
    public ResponseEntity<SubMerchantResponse> updateSubMerchant(@RequestBody SubMerchantUpdateRequest request){
        return ResponseEntity.ok(new SubMerchantResponse(subMerchantService.updateSubMerchant(request)));
    }

}
