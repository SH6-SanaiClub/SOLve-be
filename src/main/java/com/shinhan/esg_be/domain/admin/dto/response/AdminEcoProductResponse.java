package com.shinhan.esg_be.domain.admin.dto.response;

import com.shinhan.esg_be.domain.social.entity.EcoProduct;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminEcoProductResponse {

    private Long productId;
    private String name;
    private String storeName;
    private String category;
    private Long price;
    private String imageUrl;
    private String description;
    private Integer stock;
    private Boolean isActive;
    private Long purchaseCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminEcoProductResponse from(EcoProduct ecoProduct, long purchaseCount) {
        return new AdminEcoProductResponse(
                ecoProduct.getProductId(),
                ecoProduct.getName(),
                ecoProduct.getStoreName(),
                ecoProduct.getCategory(),
                ecoProduct.getPrice(),
                ecoProduct.getImageUrl(),
                ecoProduct.getDescription(),
                ecoProduct.getStock(),
                ecoProduct.getIsActive(),
                purchaseCount,
                ecoProduct.getCreatedAt(),
                ecoProduct.getUpdatedAt()
        );
    }
}
