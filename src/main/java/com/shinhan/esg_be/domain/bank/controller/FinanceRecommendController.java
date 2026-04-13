package com.shinhan.esg_be.domain.bank.controller;

import com.shinhan.esg_be.domain.bank.dto.response.SavingsRecommendResponse;
import com.shinhan.esg_be.domain.bank.service.FinanceRecommendService;
import com.shinhan.esg_be.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceRecommendController {

    private final FinanceRecommendService financeRecommendService;

    @GetMapping("/recommend")
    public ResponseEntity<SavingsRecommendResponse> recommend(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(financeRecommendService.recommend(principal.loginId()));
    }
}
