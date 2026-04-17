package com.shinhan.esg_be.domain.social.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EcoProductPurchaseItemResponse {

    private final Long purchaseId;
    private final Long productId;
    private final String name;
    private final String storeName;
    private final String category;
    private final String imageUrl;
    private final Long amount;
    private final String deliveryAddress;
    private final LocalDateTime orderedAt;

    public EcoProductPurchaseItemResponse(
            Long purchaseId,
            Long productId,
            String name,
            String storeName,
            String category,
            String imageUrl,
            Long amount,
            String deliveryAddress,
            LocalDateTime orderedAt
    ) {
        this.purchaseId = purchaseId;
        this.productId = productId;
        this.name = name;
        this.storeName = storeName;
        this.category = category;
        this.imageUrl = imageUrl;
        this.amount = amount;
        this.deliveryAddress = deliveryAddress;
        this.orderedAt = orderedAt;
    }
}
