package com.shinhan.esg_be.domain.point.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 포인트샵 구매내역 한 건을 나타내는 응답 DTO
 */
@Getter
@AllArgsConstructor
public class PointShopPurchaseHistoryItemResponse {

    private final Long userPointId;
    private final Long itemId;
    private final String itemName;
    private final String category;
    private final String imageUrl;
    private final Long usedPoints;
    private final Long pointAfter;
    private final String exchangeCode;
    private final LocalDateTime purchasedAt;
}
