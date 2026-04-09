package com.shinhan.esg_be.domain.point.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 포인트샵에서 사용하는 현재 보유 포인트 응답 DTO
 */
@Getter
@AllArgsConstructor
public class PointShopSummaryResponse {

    private final Long userId;
    private final Long totalPoints;
}
