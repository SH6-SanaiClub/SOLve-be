package com.shinhan.esg_be.domain.point.controller;

import com.shinhan.esg_be.domain.point.dto.response.PointShopPurchaseHistoryResponse;
import com.shinhan.esg_be.domain.point.service.PointShopPurchaseHistoryService;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/point-shop")
@RequiredArgsConstructor
public class PointShopPurchaseHistoryController {

    private final PointShopPurchaseHistoryService pointShopPurchaseHistoryService;
    private final AuthContext authContext;

    @GetMapping("/purchases")
    public ResponseEntity<PointShopPurchaseHistoryResponse> getPurchases() {
        return ResponseEntity.ok(pointShopPurchaseHistoryService.getPurchases(authContext.currentUserId()));
    }
}
