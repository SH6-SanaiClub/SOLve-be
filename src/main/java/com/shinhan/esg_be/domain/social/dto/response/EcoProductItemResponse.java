package com.shinhan.esg_be.domain.social.dto.response;

import lombok.Getter;

@Getter
public class EcoProductItemResponse {

    private final Long productId;
    private final String name;
    private final String category;
    private final Long price;
    private final String imageUrl;
    private final String description;
    private final Integer stock;
    private final Boolean soldOut;

    public EcoProductItemResponse(
            Long productId,
            String name,
            String category,
            Long price,
            String imageUrl,
            String description,
            Integer stock
    ) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageUrl = imageUrl;
        this.description = description;
        this.stock = stock;
        this.soldOut = stock == null || stock <= 0;
    }
}
