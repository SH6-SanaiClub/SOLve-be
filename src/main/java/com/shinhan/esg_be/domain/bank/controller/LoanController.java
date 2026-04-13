package com.shinhan.esg_be.domain.bank.controller;

import com.shinhan.esg_be.domain.bank.dto.request.LoanApplyRequest;
import com.shinhan.esg_be.domain.bank.dto.response.LoanApplyResponse;
import com.shinhan.esg_be.domain.bank.dto.response.LoanPreviewResponse;
import com.shinhan.esg_be.domain.bank.service.LoanService;
import com.shinhan.esg_be.global.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final AuthContext authContext;

    @GetMapping("/{productId}/preview")
    public ResponseEntity<LoanPreviewResponse> getLoanPreview(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(loanService.getLoanPreview(authContext.currentLoginId(), productId));
    }

    @PostMapping("/apply")
    public ResponseEntity<LoanApplyResponse> applyLoan(
            @Valid @RequestBody LoanApplyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.applyLoan(authContext.currentLoginId(), request));
    }
}
