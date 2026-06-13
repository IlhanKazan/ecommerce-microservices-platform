package com.ecommerce.paymentservice.subscription.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Yeni kayıtlı kart ekleme isteği. Kart verisi yalnızca iyzico'ya iletilir, saklanmaz.")
public record AddCardRequest(

        @Schema(description = "Kartın ekleneceği tenant", example = "12")
        Long tenantId,

        @Schema(description = "Kart için kullanıcı dostu ad", example = "İş kartım")
        String cardAlias,

        @Schema(description = "Kart sahibinin adı soyadı", example = "Ahmet Yılmaz")
        String cardHolderName,

        @Schema(description = "Kart numarası (16 hane)", example = "5528790000000008")
        String cardNumber,

        @Schema(description = "Son kullanma ay (MM)", example = "12")
        String expireMonth,

        @Schema(description = "Son kullanma yıl (YYYY)", example = "2030")
        String expireYear
) {}
