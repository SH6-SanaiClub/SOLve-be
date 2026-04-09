package com.shinhan.esg_be.domain.bank.controller;

import com.shinhan.esg_be.domain.bank.dto.response.FinanceMyResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductListResponse;
import com.shinhan.esg_be.domain.bank.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/my")
    public ResponseEntity<FinanceMyResponse> getMyFinance(
            @AuthenticationPrincipal String loginId
    ) {
        return ResponseEntity.ok(financeService.getMyFinance(loginId));
    }

    @GetMapping("/list")
    public ResponseEntity<FinanceProductListResponse> getFinanceProducts(
            @AuthenticationPrincipal String loginId,
            @RequestParam String type
    ) {
        return ResponseEntity.ok(financeService.getFinanceProducts(loginId, type));
    }
}
