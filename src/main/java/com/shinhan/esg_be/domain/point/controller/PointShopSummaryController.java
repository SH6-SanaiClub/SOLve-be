package com.shinhan.esg_be.domain.point.controller;

import com.shinhan.esg_be.domain.point.dto.response.PointShopSummaryResponse;
import com.shinhan.esg_be.domain.point.service.PointShopSummaryService;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/point-shop")
@RequiredArgsConstructor
public class PointShopSummaryController {

    private final PointShopSummaryService pointShopSummaryService;
    private final AuthContext authContext;

    @GetMapping("/summary")
    public ResponseEntity<PointShopSummaryResponse> getSummary() {
        return ResponseEntity.ok(pointShopSummaryService.getSummary(authContext.currentUserId()));
    }
}
