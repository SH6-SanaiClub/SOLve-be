package com.shinhan.esg_be.domain.point.controller;

import com.shinhan.esg_be.domain.point.dto.response.PointShopItemResponse;
import com.shinhan.esg_be.domain.point.dto.response.PointShopItemsResponse;
import com.shinhan.esg_be.domain.point.service.PointShopItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/point-shop")
@RequiredArgsConstructor
public class PointShopItemController {

    private final PointShopItemService pointShopItemService;

    @GetMapping("/items")
    public ResponseEntity<PointShopItemsResponse> getItems() {
        return ResponseEntity.ok(pointShopItemService.getItems());
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<PointShopItemResponse> getItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(pointShopItemService.getItem(itemId));
    }
}
