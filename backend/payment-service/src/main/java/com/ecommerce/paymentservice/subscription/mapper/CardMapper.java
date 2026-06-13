package com.ecommerce.paymentservice.subscription.mapper;

import com.ecommerce.paymentservice.payment.domain.AddCardCommand;
import com.ecommerce.paymentservice.subscription.controller.dto.request.AddCardRequest;
import com.ecommerce.paymentservice.subscription.controller.dto.response.CardResponse;
import com.ecommerce.paymentservice.subscription.entity.TenantCard;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {

    AddCardCommand toCommand(AddCardRequest request);

    // boolean isDefault'ın JavaBeans property adı belirsizliğini önlemek için elle map'liyoruz
    default CardResponse toResponse(TenantCard card) {
        if (card == null) {
            return null;
        }
        return new CardResponse(
                card.getId(),
                card.getCardAlias(),
                card.getLastFour(),
                card.getCardAssociation(),
                card.getCardFamily(),
                card.isDefault()
        );
    }

    List<CardResponse> toResponseList(List<TenantCard> cards);
}
