package com.ecommerce.paymentservice.subscription.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.paymentservice.common.constants.ApiPaths;
import com.ecommerce.paymentservice.common.security.TenantOwnershipGuard;
import com.ecommerce.paymentservice.subscription.controller.dto.request.AddCardRequest;
import com.ecommerce.paymentservice.subscription.controller.dto.response.CardResponse;
import com.ecommerce.paymentservice.subscription.entity.TenantCard;
import com.ecommerce.paymentservice.subscription.mapper.CardMapper;
import com.ecommerce.paymentservice.subscription.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.Card.CARDS)
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Mağaza sahibinin kayıtlı kart (iyzico vault) yönetimi — abonelik tahsilatı bu kartlardan yapılır")
public class CardController {

    private final CardService cardService;
    private final CardMapper cardMapper;
    private final TenantOwnershipGuard ownershipGuard;

    @Operation(summary = "List saved cards", description = "Tenant'ın kayıtlı kartlarını maskeli olarak döner (varsayılan kart önce).")
    @ApiResponse(responseCode = "200", description = "Kart listesi")
    @ApiResponse(responseCode = "403", description = "Mağaza sahibi değil")
    @GetMapping
    public ResponseEntity<List<CardResponse>> listCards(@RequestParam Long tenantId, @CurrentUser AuthUser user) {
        ownershipGuard.requireOwner(user, tenantId);
        return ResponseEntity.ok(cardMapper.toResponseList(cardService.listCards(tenantId)));
    }

    @Operation(summary = "Add a card", description = "iyzico vault'a yeni kart kaydeder. İlk kart otomatik varsayılan olur.")
    @ApiResponse(responseCode = "201", description = "Kart eklendi")
    @ApiResponse(responseCode = "400", description = "Kart kaydedilemedi (geçersiz kart)")
    @ApiResponse(responseCode = "403", description = "Mağaza sahibi değil")
    @PostMapping
    @Idempotent
    public ResponseEntity<CardResponse> addCard(@RequestBody AddCardRequest request, @CurrentUser AuthUser user) {
        ownershipGuard.requireOwner(user, request.tenantId());
        TenantCard card = cardService.addCard(request.tenantId(), user.email(), cardMapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(cardMapper.toResponse(card));
    }

    @Operation(summary = "Delete a card", description = "Kartı vault'tan siler. Aktif ücretli aboneliğin tek kartı silinemez (409).")
    @ApiResponse(responseCode = "204", description = "Kart silindi")
    @ApiResponse(responseCode = "404", description = "Kart bulunamadı")
    @ApiResponse(responseCode = "409", description = "Aktif aboneliğin son kartı silinemez")
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId, @RequestParam Long tenantId, @CurrentUser AuthUser user) {
        ownershipGuard.requireOwner(user, tenantId);
        cardService.deleteCard(tenantId, cardId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set default card", description = "Kartı varsayılan yapar; sonraki abonelik tahsilatları bu karttan yapılır.")
    @ApiResponse(responseCode = "200", description = "Varsayılan kart güncellendi")
    @ApiResponse(responseCode = "404", description = "Kart bulunamadı")
    @PatchMapping("/{cardId}/default")
    public ResponseEntity<Void> setDefaultCard(@PathVariable Long cardId, @RequestParam Long tenantId, @CurrentUser AuthUser user) {
        ownershipGuard.requireOwner(user, tenantId);
        cardService.setDefault(tenantId, cardId);
        return ResponseEntity.ok().build();
    }
}
