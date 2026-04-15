package com.shinhan.esg_be.domain.point.entity;

import com.shinhan.esg_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(name = "required_points", nullable = false)
    private Long requiredPoints;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    public static Item create(
            String name,
            String category,
            Long requiredPoints,
            String imageUrl,
            String description,
            Integer stock,
            Boolean isActive
    ) {
        Item item = new Item();
        item.name = name;
        item.category = category;
        item.requiredPoints = requiredPoints;
        item.imageUrl = imageUrl;
        item.description = description;
        item.stock = stock;
        item.isActive = isActive;
        return item;
    }

    public void update(
            String name,
            String category,
            Long requiredPoints,
            String imageUrl,
            String description,
            Integer stock
    ) {
        this.name = name;
        this.category = category;
        this.requiredPoints = requiredPoints;
        this.imageUrl = imageUrl;
        this.description = description;
        this.stock = stock;
    }

    public void updateStatus(boolean isActive) {
        this.isActive = isActive;
    }

    public void decreaseStock() {
        if (stock == null || stock <= 0) {
            throw new IllegalStateException("차감할 수 있는 재고가 없습니다.");
        }
        stock -= 1;
    }
}
