package com.shinhan.esg_be.domain.point.controller;

import com.shinhan.esg_be.domain.point.dto.response.PointShopPurchaseResponse;
import com.shinhan.esg_be.domain.point.service.PointShopPurchaseService;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/point-shop")
@RequiredArgsConstructor
public class PointShopPurchaseController {

    private final PointShopPurchaseService pointShopPurchaseService;
    private final AuthContext authContext;

    @PostMapping("/items/{itemId}/purchase")
    public ResponseEntity<PointShopPurchaseResponse> purchase(@PathVariable Long itemId) {
        return ResponseEntity.ok(pointShopPurchaseService.purchase(authContext.currentUserId(), itemId));
    }
}
