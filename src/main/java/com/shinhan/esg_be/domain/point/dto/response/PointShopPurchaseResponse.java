package com.shinhan.esg_be.domain.point.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 포인트샵 상품 구매 성공 후 반환하는 응답 DTO
 */
@Getter
@AllArgsConstructor
public class PointShopPurchaseResponse {

    private final Long itemId;
    private final String itemName;
    private final Long usedPoints;
    private final Long remainingPoints;
    private final String exchangeCode;
    private final LocalDateTime purchasedAt;
}
