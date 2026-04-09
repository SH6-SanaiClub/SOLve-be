package com.shinhan.esg_be.domain.point.dto.response;

import lombok.Getter;

@Getter
public class PointShopItemResponse {

    private final Long itemId;
    private final String name;
    private final String category;
    private final Long requiredPoints;
    private final String imageUrl;
    private final String description;
    private final Integer stock;
    private final Boolean soldOut;

    public PointShopItemResponse(
            Long itemId,
            String name,
            String category,
            Long requiredPoints,
            String imageUrl,
            String description,
            Integer stock
    ) {
        this.itemId = itemId;
        this.name = name;
        this.category = category;
        this.requiredPoints = requiredPoints;
        this.imageUrl = imageUrl;
        this.description = description;
        this.stock = stock;
        this.soldOut = stock == null || stock <= 0;
    }
}
