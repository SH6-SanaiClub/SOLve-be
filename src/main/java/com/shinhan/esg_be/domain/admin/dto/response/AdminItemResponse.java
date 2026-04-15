package com.shinhan.esg_be.domain.admin.dto.response;

import com.shinhan.esg_be.domain.point.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminItemResponse {

    private Long itemId;
    private String name;
    private String category;
    private Long requiredPoints;
    private String imageUrl;
    private String description;
    private Integer stock;
    private Boolean isActive;
    private Long exchangeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminItemResponse from(Item item, long exchangeCount) {
        return new AdminItemResponse(
                item.getItemId(),
                item.getName(),
                item.getCategory(),
                item.getRequiredPoints(),
                item.getImageUrl(),
                item.getDescription(),
                item.getStock(),
                item.getIsActive(),
                exchangeCount,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
