package com.shinhan.esg_be.domain.admin.controller;

import com.shinhan.esg_be.domain.admin.dto.request.AdminFinancialProductCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminFinancialProductUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminStatusUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.response.AdminFinancialProductResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminFinancialProductSubscriptionResponse;
import com.shinhan.esg_be.domain.admin.service.AdminFinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/financial-products")
@RequiredArgsConstructor
public class AdminFinanceController {

    private final AdminFinanceService adminFinanceService;

    @GetMapping
    public ResponseEntity<List<AdminFinancialProductResponse>> getFinancialProducts() {
        return ResponseEntity.ok(adminFinanceService.getFinancialProducts());
    }

    @GetMapping("/{finProductId}/subscriptions")
    public ResponseEntity<List<AdminFinancialProductSubscriptionResponse>> getSubscriptions(
            @PathVariable Long finProductId
    ) {
        return ResponseEntity.ok(adminFinanceService.getSubscriptions(finProductId));
    }

    @PostMapping
    public ResponseEntity<AdminFinancialProductResponse> createFinancialProduct(
            @RequestBody @Valid AdminFinancialProductCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminFinanceService.createFinancialProduct(request));
    }

    @PutMapping("/{finProductId}")
    public ResponseEntity<AdminFinancialProductResponse> updateFinancialProduct(
            @PathVariable Long finProductId,
            @RequestBody @Valid AdminFinancialProductUpdateRequest request
    ) {
        return ResponseEntity.ok(adminFinanceService.updateFinancialProduct(finProductId, request));
    }

    @PatchMapping("/{finProductId}/status")
    public ResponseEntity<AdminFinancialProductResponse> updateFinancialProductStatus(
            @PathVariable Long finProductId,
            @RequestBody @Valid AdminStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminFinanceService.updateFinancialProductStatus(finProductId, request));
    }
}
