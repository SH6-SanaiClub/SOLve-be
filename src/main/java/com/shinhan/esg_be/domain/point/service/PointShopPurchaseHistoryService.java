package com.shinhan.esg_be.domain.point.service;

import com.shinhan.esg_be.domain.point.dto.response.PointShopPurchaseHistoryItemResponse;
import com.shinhan.esg_be.domain.point.dto.response.PointShopPurchaseHistoryResponse;
import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointShopPurchaseHistoryService {

    private final UserPointRepository userPointRepository;

    public PointShopPurchaseHistoryResponse getPurchases(Long userId) {
        List<PointShopPurchaseHistoryItemResponse> purchases = userPointRepository
                .findPointShopPurchasesByUserIdAndReason(userId, PointReason.EXCHANGE)
                .stream()
                .map(this::toResponse)
                .toList();

        return new PointShopPurchaseHistoryResponse(purchases);
    }

    private PointShopPurchaseHistoryItemResponse toResponse(UserPoint userPoint) {
        return new PointShopPurchaseHistoryItemResponse(
                userPoint.getExchangeId(),
                userPoint.getItem().getItemId(),
                userPoint.getItem().getName(),
                userPoint.getItem().getCategory(),
                userPoint.getItem().getImageUrl(),
                Math.abs(userPoint.getChangedAmount()),
                userPoint.getPointAfter(),
                userPoint.getExchangeCode(),
                userPoint.getCreatedAt()
        );
    }
}
