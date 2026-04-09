package com.shinhan.esg_be.domain.point.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 포인트샵 구매내역 목록 응답 DTO
 */
@Getter
@AllArgsConstructor
public class PointShopPurchaseHistoryResponse {

    private final List<PointShopPurchaseHistoryItemResponse> purchases;
}
