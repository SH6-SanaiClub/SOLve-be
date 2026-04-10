package com.shinhan.esg_be.domain.bank.controller;

import com.shinhan.esg_be.domain.bank.dto.response.FinanceHistoryResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceMyResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductListResponse;
import com.shinhan.esg_be.domain.bank.service.FinanceService;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;
    private final AuthContext authContext;

    @GetMapping("/my")
    public ResponseEntity<FinanceMyResponse> getMyFinance() {
        return ResponseEntity.ok(financeService.getMyFinance(authContext.currentLoginId()));
    }

    @GetMapping("/history")
    public ResponseEntity<FinanceHistoryResponse> getFinanceHistory() {
        return ResponseEntity.ok(financeService.getFinanceHistory(authContext.currentLoginId()));
    }

    @GetMapping("/list")
    public ResponseEntity<FinanceProductListResponse> getFinanceProducts(
            @RequestParam String type
    ) {
        return ResponseEntity.ok(financeService.getFinanceProducts(authContext.currentLoginId(), type));
    }
}
